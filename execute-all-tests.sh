#!/usr/bin/env bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source ${SCRIPT_DIR}/docker/dev.env

echo "Execute all tests"

SCHEMA_VERSION=${1:-latest} \
docker-compose \
--env-file ${SCRIPT_DIR}/docker/dev.env \
-f docker/docker-compose-postgres-test.yml \
-f docker/docker-compose-solrcloud.yml \
-f docker/docker-compose-gradle.yml \
run --rm --service-ports \
scxa-gradle bash -c "
set -e

./gradlew clean

./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
atlas-web-core:testClasses

./gradlew -PtestResultsPath=ut :atlas-web-core:test --tests *Test
#sh./gradlew -PtestResultsPath=it :atlas-web-core:test --tests *IT
./gradlew :atlas-web-core:jacocoTestReport

./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
app:testClasses

./gradlew -PtestResultsPath=ut :app:test --tests *Test
./gradlew -PtestResultsPath=it -PexcludeTests=**/*WIT.class :app:test --tests *IT
./gradlew -PtestResultsPath=e2e :app:test --tests *WIT
./gradlew :app:jacocoTestReport
"
