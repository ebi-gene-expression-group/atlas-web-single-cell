#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ENV_FILE="${SCRIPT_DIR}/docker/dev.env"
source "${ENV_FILE}"

function show_usage {
  echo "Usage: build-and-deploy-webapp.sh [OPTION]..."
  echo "It is building and deploying our web application."
  echo ""
  echo "All options are disabled if omitted."
  echo -e "-n\tUse this flag if you would not like to do any build, just execute the application."
  echo -e "-f\tUse this flag if you would like to build the front-end javascript packages."
  echo -e "-b\tUse this flag if you would like to build the back-end of the web application."
  echo -e "-l\tFast local frontend mode: reuse existing JS dependencies and skip install/audit/upgrade steps."
  echo -e "-h\tDisplaying this help file."
  echo -e "\nIf you don't give any flags or you add both then the script is going to build both front and back-end part of the web application."
}

function get_build_type() {
  if [[ ${BUILD_FRONTEND:-} == "true" && ${BUILD_BACKEND:-} != "true" ]]; then
    echo "-ui-only"
  elif [[ ${BUILD_FRONTEND:-} != "true" && ${BUILD_BACKEND:-} == "true" ]]; then
    echo "-war-only"
  elif [[ ${BUILD_FRONTEND:-} == "true" && ${BUILD_BACKEND:-} == "true" ]] || [[ -z ${BUILD_FRONTEND:-} && -z ${BUILD_BACKEND:-} ]]; then
    echo "-all"
  elif [[ ${BUILD_FRONTEND:-} == "false" && ${BUILD_BACKEND:-} == "false" ]]; then
    echo "-no"
  fi
}

function ensure_colima {
  command -v colima >/dev/null 2>&1 || { echo "colima is not installed"; exit 1; }
  command -v docker >/dev/null 2>&1 || { echo "docker CLI is not installed"; exit 1; }

  # Start Colima with Docker runtime.
  # Add --disk <size-in-GiB> here if you need more space.
  colima start --runtime docker >/dev/null

  # Make sure the Docker CLI points at Colima.
  docker context use colima >/dev/null 2>&1 || true
}

function detect_compose_cmd {
  if docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
  else
    echo "Neither 'docker compose' nor 'docker-compose' is available."
    exit 1
  fi
}

while getopts ":bfhln" opt; do
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
    l)
      FAST_LOCAL_FRONTEND=true
      ;;
    h)
      show_usage
      exit 0
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      echo ""
      show_usage
      exit 1
      ;;
  esac
done

ensure_colima
detect_compose_cmd

BUILD_POSTFIX=$(get_build_type)
FRONTEND_BUILD_FLAGS="-i"

if [[ ${FAST_LOCAL_FRONTEND:-} == "true" ]]; then
  FRONTEND_BUILD_FLAGS="-l"
fi

DOCKER_COMPOSE_COMMAND=(
  "${COMPOSE_CMD[@]}"
  --project-name "${PROJECT_NAME}"
  --env-file "${ENV_FILE}"
  -f ./docker/docker-compose-solrcloud.yml
  -f ./docker/docker-compose-postgres.yml
  -f ./docker/docker-compose-tomcat.yml
  -f ./docker/docker-compose-build"${BUILD_POSTFIX}".yml
)

SCHEMA_VERSION=latest FRONTEND_BUILD_FLAGS="${FRONTEND_BUILD_FLAGS}" "${DOCKER_COMPOSE_COMMAND[@]}" up -d

printf '%b\n\n' "👀 Keep an eye on Tomcat logs at container scxa-tomcat: docker logs -f scxa-tomcat-1"
printf '%b\n' "🧹 Press Enter to stop and remove the containers, or Ctrl+C to cancel..."
read -r -s

SCHEMA_VERSION=latest FRONTEND_BUILD_FLAGS="${FRONTEND_BUILD_FLAGS}" "${DOCKER_COMPOSE_COMMAND[@]}" down
