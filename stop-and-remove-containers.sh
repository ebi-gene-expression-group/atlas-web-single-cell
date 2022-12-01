#!/usr/bin/env bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source ${SCRIPT_DIR}/docker/dev.env

docker stop \
${SOLR_CLOUD_CONTAINER_1_NAME} ${SOLR_CLOUD_CONTAINER_2_NAME} \
${SOLR_CLOUD_ZK_CONTAINER_1_NAME} ${SOLR_CLOUD_ZK_CONTAINER_2_NAME} ${SOLR_CLOUD_ZK_CONTAINER_3_NAME} \
${POSTGRES_HOST} scxa-flyway-test \
scxa-gradle

docker rm \
${SOLR_CLOUD_CONTAINER_1_NAME} ${SOLR_CLOUD_CONTAINER_2_NAME} \
${SOLR_CLOUD_ZK_CONTAINER_1_NAME} ${SOLR_CLOUD_ZK_CONTAINER_2_NAME} ${SOLR_CLOUD_ZK_CONTAINER_3_NAME} \
${POSTGRES_HOST} scxa-flyway-test \
scxa-gradle
