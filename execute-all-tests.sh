#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "Execute all tests in atlas-web-core"
ENV_FILE=${SCRIPT_DIR}/atlas-web-core/docker/dev.env
source ${ENV_FILE}

SCHEMA_VERSION=${1:-latest} \
docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
gradle bash -c "
set -e

gradle clean

gradle \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/exp \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
-PsolrUser=${SOLR_USER} \
-PsolrPassword=${SOLR_PASSWORD} \
:atlas-web-core:testClasses

gradle -PtestResultsPath=ut :atlas-web-core:test --tests *Test
gradle -PsolrUser=${SOLR_USER} -PsolrPassword=${SOLR_PASSWORD} -PtestResultsPath=it :atlas-web-core:test --tests *IT
gradle :atlas-web-core:jacocoTestReport
"

SCHEMA_VERSION=${1:-latest} \
docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
down

#######################

echo "Execute all tests in app"
ENV_FILE=${SCRIPT_DIR}/docker/dev.env
source ${ENV_FILE}

SCHEMA_VERSION=${1:-latest} \
docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
gradle bash -c "
set -e

gradle clean

gradle \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/exp \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${PROJECT_NAME}-${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${PROJECT_NAME}-${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
-PsolrUser=${SOLR_USER} \
-PsolrPassword=${SOLR_PASSWORD} \
:app:testClasses

gradle -PtestResultsPath=ut :app:test --tests *Test
gradle -PsolrUser=${SOLR_USER} -PsolrPassword=${SOLR_PASSWORD} -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT
gradle -PsolrUser=${SOLR_USER} -PsolrPassword=${SOLR_PASSWORD} -PtestResultsPath=e2e :app:test --tests *WIT
gradle :app:jacocoTestReport"

SCHEMA_VERSION=${1:-latest} \
docker compose \
--project-name ${PROJECT_NAME} \
--env-file ${ENV_FILE} \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
down
