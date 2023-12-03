#!/usr/bin/env python3
import json
import os
import tempfile
import threading
import typing

import boto3
from flask import Flask

import podcast
import rmq
import utils
from common import *


def s3_uri(bucket_name: str, upload_key: str) -> str:
    return f's3://{bucket_name}/{upload_key}'


def s3_download(s3, bucket_name: str, key: str, local_fn: str):
    s3.meta.client.download_file(bucket_name, key, local_fn)
    assert os.path.exists(local_fn), f"the local file {local_fn} should have been downloaded"
    return local_fn


def background_thread():
    requests_q = 'podcast-processor-requests'
    aws_region_env = os.environ["AWS_REGION"]
    aws_key_id = os.environ['AWS_ACCESS_KEY_ID']
    aws_key_secret = os.environ['AWS_ACCESS_KEY_SECRET']
    rmq_uri = utils.parse_uri(os.environ['RMQ_ADDRESS'])
    boto3.setup_default_session(
        aws_secret_access_key=aws_key_secret,
        aws_access_key_id=aws_key_id,
        region_name=aws_region_env)

    s3 = boto3.resource("s3")

    utils.log('-' * 50)
    for b in s3.buckets.all():
        utils.log(b)
    utils.log('-' * 50)

    def handle_job(properties, request) :
        utils.log('-' * 50)
        utils.log(properties)
        utils.log(request)

        def resolve_config_file_name() -> str:
            bp_mode: str = os.environ.get("BP_MODE", "development")
            utils.log("BP_MODE: %s" % bp_mode)
            return "config-%s.json" % bp_mode

        config = utils.load_config(resolve_config_file_name())

        assets_s3_bucket = config["podcast-assets-s3-bucket"]
        assets_s3_bucket_folder = config["podcast-assets-s3-bucket-folder"]
        output_s3_bucket = config["podcast-output-s3-bucket"]

        json_request = json.loads(request['request'])
        title = json_request['title']
        uid = json_request['uid']
        description = json_request['description']
        request_uploads = json_request['uploads']
        intro_media = request_uploads['INTRODUCTION']
        interview_media = request_uploads['INTERVIEW']
        image_media = request_uploads['IMAGE']
        simple_request = {
            'image': image_media,
            'intro': intro_media,
            'interview': interview_media,
            'title': title,
            'uid': uid,
            'description': description
        }
        utils.log(f'processing: {simple_request}')
        normalized_uid_str = normalize_string(uid)

        tmpdir = os.path.join(tempfile.gettempdir(), normalized_uid_str)
        output_dir = os.path.join(tmpdir, "output")

        def build_full_s3_asset_path_for(fn: str):
            return s3_uri(assets_s3_bucket, f'{assets_s3_bucket_folder}/{fn}')

        asset_closing = build_full_s3_asset_path_for("closing.mp3")
        asset_intro = build_full_s3_asset_path_for("intro.mp3")
        asset_music_segue = build_full_s3_asset_path_for("music-segue.mp3")
        utils.log(f'asset: closing: {asset_closing}')
        utils.log(f'asset: intro: {asset_intro}')
        utils.log(f'asset: segue: {asset_music_segue}')

        def download(s3p: str) -> str:
            import typing
            utils.log("going to download %s" % s3p)
            parts: typing.List[str] = s3p.split("/")
            bucket, folder, fn = parts[2:]
            local_fn: str = os.path.join(tmpdir, "downloads", bucket, folder, fn)
            the_directory: str = os.path.dirname(local_fn)
            if not os.path.exists(the_directory):
                os.makedirs(the_directory)
            assert os.path.exists(the_directory), f"the directory {the_directory} should exist but does not."
            utils.log("going to download %s to %s" % (s3p, local_fn))
            try:
                s3_download(s3, bucket, os.path.join(folder, fn), local_fn)
                assert os.path.exists(local_fn), (
                        "the file should be downloaded to %s, but was not." % local_fn
                )
            except BaseException as e:
                utils.log('something has gone horribly awry when trying to download the S3 file: %s' % e)

            return local_fn

        downloaded_files = {}

        for s3_path in [
            asset_closing,
            asset_intro,
            asset_music_segue,
            intro_media,
            interview_media,
        ]:
            downloaded_files[s3_path] = download(s3_path)

        utils.reset_and_recreate_directory(output_dir)
        results = podcast.create_podcast(
            downloaded_files[asset_intro],
            downloaded_files[asset_music_segue],
            downloaded_files[asset_closing],
            downloaded_files[intro_media],
            downloaded_files[interview_media],
            output_dir,
        )
        utils.log(f'results: {results}')
        upload_local_fn = results['export']
        fqn = f'{uid}/{os.path.basename(upload_local_fn)}'
        s3.meta.client.upload_file(upload_local_fn, output_s3_bucket, fqn)
        x=  {
            'title': title,
            'description': description,
            'introduction': intro_media,
            'interview': interview_media,
            'uid': uid,
            'exported-audio': s3_uri(output_s3_bucket, fqn),
            'exported-photo': image_media
        }
        return x

    while True:
        try:
            utils.log(rmq_uri)
            rmq.start_rabbitmq_processor(
                requests_q,
                rmq_uri["host"],
                rmq_uri["username"],
                rmq_uri["password"],
                rmq_uri["path"],
                handle_job,
            )
        except Exception as ex:
            utils.exception(
                ex,
                message="There was some sort of error installing a RabbitMQ listener. Restarting the processor... ",
            )


if __name__ == "__main__":

    def run_flask():
        app = Flask(__name__)

        @app.route("/")
        def hello():
            return json.dumps({"status": "HODOR"})

        utils.log("about to start the Flask service")
        app.run(port=8080)


    def run_rmq():

        retry_count = 0
        max_retries = 5
        while retry_count < max_retries:
            try:
                retry_count += 1
                utils.log("launching RabbitMQ background thread")
                background_thread()
            except Exception as e:
                utils.exception(
                    e,
                    message="something went wrong trying to start the RabbitMQ processing thread!",
                )

        utils.log("Exhausted retry count of %s times." % max_retries)


    for f in [run_flask, run_rmq]:
        threading.Thread(target=f).start()

    utils.log("launched RabbitMQ and Flask threads.")
