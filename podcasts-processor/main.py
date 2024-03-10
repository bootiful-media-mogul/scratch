#!/usr/bin/env python3
import json
import os
import uuid

import boto3
from flask import Flask

import podcast
import rmq
import utils


def download(s3, s3p: str) -> str:
    import typing
    utils.log("going to download %s" % s3p)
    parts: typing.List[str] = s3p.split("/")
    bucket, folder, fn = parts[2:]
    local_fn: str = os.path.join(os.environ['HOME'], "podcast-episode-s3-downloads", bucket, folder, fn)
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


def s3_uri(bucket_name: str, upload_key: str) -> str:
    return f's3://{bucket_name}/{upload_key}'


def s3_download(s3, bucket_name: str, key: str, local_fn: str):
    s3.meta.client.download_file(bucket_name, key, local_fn)
    assert os.path.exists(local_fn), f"the local file {local_fn} should have been downloaded"
    return local_fn


def handle_podcast_episode_creation_request(s3, request: str):
    incoming_json_request = json.loads(request)
    segment_s3_uris = incoming_json_request['segments']
    tmp_dir = os.path.join(os.environ['HOME'], 'podcast-production', str(uuid.uuid4()))
    os.makedirs(tmp_dir, exist_ok=True)
    local_files = [download(s3, s3_uri) for s3_uri in segment_s3_uris]
    local_files_segments = [podcast.Segment(lf, os.path.splitext(lf)[1], crossfade_time=100) for lf in local_files]
    print(local_files_segments)


def build_s3_client() -> boto3.Session:
    aws_region = os.environ["AWS_REGION"]
    aws_key_id = os.environ['AWS_ACCESS_KEY_ID']
    aws_key_secret = os.environ['AWS_ACCESS_KEY_SECRET']

    def good_string(s: str) -> bool:
        if s is not None and isinstance(s, str):
            if s.strip() != '':
                return True
        return False

    for k, v in {'access-key-secret': aws_key_secret,
                 'access-key-id': aws_key_id,
                 'access-region': aws_region}.items():
        assert good_string(v), f'the value for {k} is invalid'

    boto3.setup_default_session(
        aws_secret_access_key=aws_key_secret,
        aws_access_key_id=aws_key_id,
        region_name=aws_region)

    s3 = boto3.resource("s3")
    return s3


def background_thread():
    requests_q = 'podcast-processor-requests'
    rmq_uri = utils.parse_uri(os.environ['RMQ_ADDRESS'])
    s3 = build_s3_client()
    utils.log('-' * 50)
    for b in s3.buckets.all():
        utils.log(b)
    utils.log('-' * 50)

    # def old_handle_job(properties, request):
    #     json_request = json.loads(request)
    #     uid = str(uuid.uuid4())
    #     intro_media = json_request['introduction']
    #     interview_media = json_request['interview']
    #     output_media = json_request['output']
    #
    #     simple_request = {
    #         'intro': intro_media,
    #         'interview': interview_media,
    #         'output': output_media
    #     }
    #     utils.log(f'processing: {simple_request}')
    #     normalized_uid_str = normalize_string(uid)
    #
    #     tmpdir = os.path.join(tempfile.gettempdir(), normalized_uid_str)
    #     output_dir = os.path.join(tmpdir, "output")
    #
    #     def build_full_s3_asset_path_for(fn: str):
    #         return s3_uri(assets_s3_bucket, f'{assets_s3_bucket_folder}/{fn}')
    #
    #     asset_closing = build_full_s3_asset_path_for("closing.mp3")
    #     asset_intro = build_full_s3_asset_path_for("intro.mp3")
    #     asset_music_segue = build_full_s3_asset_path_for("music-segue.mp3")
    #
    #     downloaded_files = {}
    #
    #     for s3_path in [
    #         asset_closing,
    #         asset_intro,
    #         asset_music_segue,
    #         intro_media,
    #         interview_media,
    #     ]:
    #         downloaded_files[s3_path] = download(s3_path)
    #
    #     utils.reset_and_recreate_directory(output_dir)
    #     results = podcast.create_podcast(
    #         downloaded_files[asset_intro],
    #         downloaded_files[asset_music_segue],
    #         downloaded_files[asset_closing],
    #         downloaded_files[intro_media],
    #         downloaded_files[interview_media],
    #         output_dir,
    #     )
    #     utils.log(f'results: {results}')
    #     upload_local_fn = results['export']
    #     print('output s3 url: ', output_media)
    #     bucket, folder, file = output_media[len('s3://'):].split('/')
    #     s3.meta.client.upload_file(upload_local_fn, bucket, f'{folder}/{file}')
    #     return {'exported': output_media}

    while True:
        try:
            utils.log(rmq_uri)
            rmq.start_rabbitmq_processor(
                requests_q,
                rmq_uri["host"],
                rmq_uri["username"],
                rmq_uri["password"],
                rmq_uri["path"],
                handle_podcast_episode_creation_request,
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
        #  todo does k8s need to know about this port?
        app.run(port=7070)


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


    base = os.path.join('podcast-assets-bucket-dev', '062019')
    assets = ['intro.mp3', '1.aiff', '2.aiff'  'music-segue.mp3', '3.aiff', '4.aiff', 'closing.mp3']
    s3_uris = [f's3://{base}/{p}' for p in assets]
    json_request = json.dumps({
        'segments': s3_uris
    })
    print(json_request)
    handle_podcast_episode_creation_request(json_request)

    # todo restore this
    # for f in [run_flask, run_rmq]:
    #     threading.Thread(target=f).start()

    utils.log("launched RabbitMQ and Flask threads.")
