#!/usr/bin/env bash


docker run \
 -e RMQ_ADDRESS=amqp://bp-rmq:bp-rmq@host.docker.internal/ \
 -e AWS_REGION=us-east-1 \
 -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
 -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
 audio-processor
