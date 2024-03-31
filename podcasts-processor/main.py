#!/usr/bin/env python3
import json
import logging
import os
import typing
import uuid
import pika
import boto3
from flask import Flask
import threading
import podcast
import rmq
import utils


def download(s3, s3p: str, output_file: str) -> str:
    def s3_download(s3, bucket_name: str, key: str, local_fn: str):
        s3.meta.client.download_file(bucket_name, key, local_fn)
        assert os.path.exists(local_fn), f"the local file {local_fn} should have been downloaded"
        return local_fn

    import typing
    parts: typing.List[str] = s3p.split("/")
    bucket, folder, fn = parts[2:]
    local_fn: str = output_file
    the_directory: str = os.path.dirname(local_fn)
    if not os.path.exists(the_directory):
        os.makedirs(the_directory)
    assert os.path.exists(the_directory), f"the directory {the_directory} should exist but does not."
    utils.log("going to download %s to %s" % (s3p, local_fn))
    try:
        utils.log(f'going to download {bucket}/{folder}/{fn}')
        s3_download(s3, bucket, os.path.join(folder, fn), local_fn)
        assert os.path.exists(local_fn), (
                "the file should be downloaded to %s, but was not." % local_fn
        )
    except BaseException as e:
        utils.log('something has gone horribly awry when trying to download the S3 file: %s' % e)

    return local_fn


def s3_uri(bucket_name: str, upload_key: str) -> str:
    return f's3://{bucket_name}/{upload_key}'


def handle_podcast_episode_creation_request(s3,
                                            properties: pika.BasicProperties,
                                            incoming_json_request: typing.Any,
                                            uid: str):
    # incoming_json_request = json.loads(request)
    print(f'''incoming request: {incoming_json_request}''')
    output_s3_uri = incoming_json_request['outputS3Uri']
    segments = incoming_json_request['segments']
    segments = [a['s3Uri'] for a in segments]
    tmp_dir = os.path.join(os.environ['HOME'], 'podcast-production', uid)
    os.makedirs(tmp_dir, exist_ok=True)
    local_files = [download(s3, s3_uri, os.path.join(tmp_dir, s3_uri.split('/')[-1])) for s3_uri in segments]
    local_files_segments = [podcast.Segment(lf, os.path.splitext(lf)[1][1:], crossfade_time=100) for lf in local_files]
    output_podcast_audio_local_fn = podcast.create_podcast(local_files_segments,
                                                           os.path.join(tmp_dir, 'output.mp3'),
                                                           output_extension='mp3')
    utils.log(f'the produced audio is stored locally {output_podcast_audio_local_fn}')

    s3_parts = output_s3_uri[len('s3://'):]
    utils.log(s3_parts)
    bucket, folder, file = s3_parts.split('/')
    s3.meta.client.upload_file(output_podcast_audio_local_fn, bucket, f'{folder}/{file}')
    return {'outputS3Uri': output_s3_uri}
    # todo upload this back to S3


def build_s3_client() -> typing.Any:
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


if __name__ == "__main__":

    # todo remove this function and its use; it's just for prototyping and shouldn't be in the final code
    #
    # def handle_sample_request():
    #     '''
    #     {'uid': '96fdf0d0-c4d0-40de-bf9b-fb3de00f61f6', 'episodeId': 28, 'outputUri': 's3://mogul-podcast-episodes/2a553674-5663-4175-8e7b-8e67eb13d7fc/173d0620-d917-44fd-84d9-c69967a4f3c0',
    #      'segments': [{'index': 0, 's3Uri': 's3://mogul-podcast-episodes/a977015c-2668-459c-b56e-556ccf6bbae3/cac1f3c8-85ab-4cf3-bcc4-b51488888815', 'crossfade': 0}]}
    #
    #     '''
    #     base = os.path.join('podcast-assets-bucket-dev', '062019')
    #     assets = ['intro.mp3', '1.aiff', '2.aiff', 'music-segue.mp3', '3.aiff', '4.aiff', 'closing.mp3']
    #     s3_uris = [f's3://{base}/{p}' for p in assets]
    #     my_uid = str(uuid.uuid4())
    #     json_request = json.dumps(
    #         {'segments': s3_uris, 'output_s3_uri': f's3://podcast-output-bucket-dev/{my_uid}/output.mp3'})
    #     print(json_request)
    #     # s3 = build_s3_client()
    #     # handle_podcast_episode_creation_request(s3, json_request, my_uid)
    #
    #
    # handle_sample_request()

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
                requests_q = 'podcast-processor-requests'
                rmq_uri = utils.parse_uri(os.environ['RMQ_ADDRESS'])
                s3 = build_s3_client()

                def handler(properties, json_request) -> str:
                    return handle_podcast_episode_creation_request(
                        s3, properties, json_request, str(uuid.uuid4()))

                while True:
                    try:
                        utils.log(rmq_uri)
                        rmq.start_rabbitmq_processor(
                            requests_q,
                            rmq_uri["host"],
                            rmq_uri["username"],
                            rmq_uri["password"],
                            rmq_uri["path"],
                            handler,
                        )
                    except Exception as ex:
                        utils.exception(
                            ex,
                            message="There was some sort of error "
                                    "installing a RabbitMQ listener."
                                    "Restarting the processor... ",
                        )
            except Exception as e:
                utils.exception(
                    e,
                    message="something went wrong trying to "
                            "start the RabbitMQ processing thread!",
                )

        utils.log("Exhausted retry count of %s times." % max_retries)


    for f in [run_flask, run_rmq]:
        threading.Thread(target=f).start()
