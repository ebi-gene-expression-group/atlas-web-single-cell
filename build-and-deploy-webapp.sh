#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

ENV_FILE=${SCRIPT_DIR}/docker/dev.env
source ${ENV_FILE}

DOCKER_COMPOSE_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file=${ENV_FILE} \
-f ./docker/docker-compose-solrcloud.yml \
-f ./docker/docker-compose-postgres.yml \
-f ./docker/docker-compose-tomcat.yml \
-f ./docker/docker-compose-build.yml"

DOCKER_COMPOSE_COMMAND_VARS="SCHEMA_VERSION=latest"

eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up -d"

printf '%b\n\n' "👀 Keep an eye on Tomcat logs at container scxa-tomcat: docker logs -f scxa-tomcat-1"
printf '%b\n' "🧹 Press Enter to stop and remove the containers, or Ctrl+C to cancel..."
read -r -s

eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down"
