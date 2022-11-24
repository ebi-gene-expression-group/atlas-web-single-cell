#!/usr/bin/env bash

for _SPECIES in ${SPECIES}
do
  rsync -av --include=${_SPECIES}* --include=*/ --exclude=* /atlas-data/bioentity_properties /root
done

cd /root/atlas-web-bulk
./gradlew -PdataFilesLocation=/root \
-PexperimentFilesLocation=/atlas-data/scxa \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="bioentities-json --output=/root/bioentity-properties-jsonl"

cd /root/index-bioentities/bin
./create-bioentities-collection.sh
./create-bioentities-schema.sh
./create-bioentities-suggesters.sh

export SOLR_COLLECTION=${SOLR_COLLECTION_BIOENTITIES}
export SCHEMA_VERSION=${SOLR_COLLECTION_BIOENTITIES_SCHEMA_VERSION}
for FILE in `ls /root/bioentity-properties-jsonl/*.jsonl`
do
  INPUT_JSONL=${FILE} ./solr-jsonl-chunk-loader.sh >> /dev/stdout 2>&1
done
./build-suggesters.sh
unset SOLR_COLLECTION
unset SCHEMA_VERSION

cd /root/index-scxa/bin
export PATH=.:${PATH}
./upload-biosolr-lib.sh
./create-scxa-analytics-collection.sh
./create-scxa-analytics-schema.sh
./create-scxa-analytics-suggesters.sh
for EXP_ID in ${EXP_IDS}
do
  CONDENSED_SDRF_TSV=/atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.condensed-sdrf.tsv \
  ./load-scxa-analytics.sh
done
./build-scxa-analytics-suggestions.sh

./create-scxa-gene2experiment-collection.sh
./create-scxa-gene2experiment-schema.sh
for EXP_ID in ${EXP_IDS}
do
  EXP_ID=${EXP_ID} \
  MATRIX_MARKT_ROWS_GENES_FILE=/atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.aggregated_filtered_normalised_counts.mtx_rows \
  ./load-scxa-gene2experiment.sh
done

sleep 3h
