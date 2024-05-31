#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

ENV_FILE=${SCRIPT_DIR}/docker/dev.env
source ${ENV_FILE}

function show_usage {
  echo "Usage: build-and-deploy-webapp.sh [OPTION]..."
  echo "It is building and deploying our web application."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-n\tUse this flag if you would not like to do any build, just execute the application."
  echo -e "-f\tUse this flag if you would like to build the front-end javascript packages."
  echo -e "-b\tUse this flag if you would like to build the back-end of the web application."
  echo -e "-h\tDisplaying this help file."
  echo -e "\nIf you don't give any flags or you add both then the script is going to build both front and back-end part of the web application."
}

function get_build_type() {
  if [[ $BUILD_FRONTEND == "true" && $BUILD_BACKEND != "true" ]]; then
    echo "-ui-only"
  elif [[ $BUILD_FRONTEND != "true" && $BUILD_BACKEND == "true" ]]; then
    echo "-war-only"
  elif [[ $BUILD_FRONTEND == "true" && $BUILD_BACKEND == "true" ]] || [[ -z $BUILD_FRONTEND && -z $BUILD_BACKEND ]]; then
    echo "-all"
  elif [[ $BUILD_FRONTEND == "false" && $BUILD_BACKEND == "false" ]]; then
    echo "-no"
  fi
}

while getopts ":bfhn" opt; do
  case $opt in
    n)
      BUILD_FRONTEND=false
      BUILD_BACKEND=false
      ;;
    f)
      BUILD_FRONTEND=true
      ;;
    b)
      BUILD_BACKEND=true
      ;;
    h)
      show_usage
      exit 1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      show_usage
      exit 1
      ;;
  esac
done

BUILD_POSTFIX=$(get_build_type)

DOCKER_COMPOSE_COMMAND="docker compose \
--project-name ${PROJECT_NAME} \
--env-file=${ENV_FILE} \
-f ./docker/docker-compose-solrcloud.yml \
-f ./docker/docker-compose-postgres.yml \
-f ./docker/docker-compose-tomcat.yml \
-f ./docker/docker-compose-build${BUILD_POSTFIX}.yml"

DOCKER_COMPOSE_COMMAND_VARS="SCHEMA_VERSION=latest"

eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "up -d"

printf '%b\n\n' "👀 Keep an eye on Tomcat logs at container scxa-tomcat: docker logs -f scxa-tomcat-1"
printf '%b\n' "🧹 Press Enter to stop and remove the containers, or Ctrl+C to cancel..."
read -r -s

eval "${DOCKER_COMPOSE_COMMAND_VARS}" "${DOCKER_COMPOSE_COMMAND}" "down"
