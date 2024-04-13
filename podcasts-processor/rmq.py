#!/usr/bin/env python

import json

import pika

import utils


def start_rabbitmq_processor(
        requests_q: str,
        rabbit_host: str,
        rabbit_username: str,
        rabbit_password: str,
        rabbit_vhost: str,
        process_job_requests_fn):
    utils.log(
        f"Establishing a connection to RabbitMQ host '{rabbit_host}', "
        "having virtual host '{rabbit_vhost}', with username "
        "'{rabbit_username}'.".strip()
    )

    if rabbit_vhost is not None:
        rabbit_vhost = rabbit_vhost.strip()
        if rabbit_vhost == "/" or rabbit_vhost == "":
            rabbit_vhost = None

    if rabbit_vhost is None:
        params = pika.ConnectionParameters(
            host=rabbit_host,
            credentials=pika.PlainCredentials(rabbit_username, rabbit_password),
            heartbeat=0
        )
    else:
        utils.log('vhost is not None')
        params = pika.ConnectionParameters(
            host=rabbit_host,
            virtual_host=rabbit_vhost,
            credentials=pika.PlainCredentials(rabbit_username, rabbit_password),
            heartbeat=0
        )

    with pika.BlockingConnection(params) as connection:
        with connection.channel() as channel:
            for method_frame, properties, json_request in channel.consume(requests_q):
                try:
                    replies_q = str(properties.reply_to)
                    assert replies_q is not None and replies_q.strip() != '', 'the replies queue must be non empty'
                    loads = json.loads(json_request)  #
                    result = process_job_requests_fn(properties, loads)
                    json_response: str = json.dumps(result)
                    utils.log(f'sending json_response {json_response} to reply queue {replies_q}')
                    basic_properties = pika.BasicProperties(
                        correlation_id=properties.correlation_id,
                        content_type="text/plain",
                        delivery_mode=1,
                    )
                    channel.basic_publish(
                        '',
                        replies_q,
                        json_response,
                        basic_properties
                    )
                    channel.basic_ack(method_frame.delivery_tag)

                except BaseException as ex:
                    utils.exception(ex, "something went terribly awry in processing the message")
