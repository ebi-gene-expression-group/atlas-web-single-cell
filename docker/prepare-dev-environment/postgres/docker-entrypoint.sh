#!/usr/bin/env bash

cd /root/atlas-web-single-cell

# Creating the experiments is required because certain scripts below use experiment.accession as a foreign key
./gradlew \
-PdataFilesLocation=${ATLAS_DATA} \
-PexperimentFilesLocation=${ATLAS_DATA_SCXA_DEST} \
-PexperimentDesignLocation=${ATLAS_DATA_SCXA_EXPDESIGN_DEST} \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=solr-foo:9983 \
-PsolrHosts=http://solr-foo:8983/solr \
:cli:bootRun --args="create-update-experiment -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

# Task create-update-experiment will write the experiment design file if the experiment is new, but we want to re-use
# this script to update experiments too
./gradlew \
-PdataFilesLocation=${ATLAS_DATA} \
-PexperimentFilesLocation=${ATLAS_DATA_SCXA_DEST} \
-PexperimentDesignLocation=${ATLAS_DATA_SCXA_EXPDESIGN_DEST} \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=solr-foo:9983 \
-PsolrHosts=http://solr-foo:8983/solr \
:cli:bootRun --args="update-experiment-design -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

# The last commit before v19 was introduced...
if [ ${SCHEMA_VERSION} = 18 ] ; then
  cd /root/db-scxa
  git checkout 1236753d3d799effa4d24fa9bdfb9292c66309ab
fi

cd /root/db-scxa/bin
export PATH=.:$PATH

for EXP_ID in ${EXP_IDS}
do
  EXP_ID=${EXP_ID} EXPERIMENT_MATRICES_PATH=${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID} ./load_db_scxa_analytics.sh

  if [ ${SCHEMA_VERSION} = 18 ]; then
    EXP_ID=${EXP_ID} EXPERIMENT_DIMRED_PATH=${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID} ./load_db_scxa_dimred.sh
  else
    # Copied from db-scxa/tests/test-dimred-load.sh
    ls ${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID}/${EXP_ID}.tsne*.tsv | while read -r l; do
      DIMRED_TYPE=tsne
      DIMRED_FILE_PATH=$l
      DIMRED_PARAM_JSON='[{"perplexity": '$(echo "$l" | sed 's/.*[^0-9]\([0-9]*\).tsv/\1/g')'}]'
      load_db_scxa_dimred.sh ${dbConnection} ${EXP_ID} ${DIMRED_TYPE} ${DIMRED_FILE_PATH} "${DIMRED_PARAM_JSON}"
    done

    ls ${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID}/${EXP_ID}.umap*.tsv | while read -r l; do
      DIMRED_TYPE=umap
      DIMRED_FILE_PATH=$l
      DIMRED_PARAM_JSON='[{"n_neighbors": '$(echo "$l" | sed 's/.*[^0-9]\([0-9]*\).tsv/\1/g')'}]'
      load_db_scxa_dimred.sh ${dbConnection} ${EXP_ID} ${DIMRED_TYPE} ${DIMRED_FILE_PATH} "${DIMRED_PARAM_JSON}"
    done
  fi

  EXP_ID=${EXP_ID} \
  EXPERIMENT_CLUSTERS_FILE=${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID}/${EXP_ID}.clusters.tsv \
  CONDENSED_SDRF_TSV=${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID}/${EXP_ID}.condensed-sdrf.tsv \
  ./load_db_scxa_cell_clusters.sh

  EXP_ID=${EXP_ID} \
  EXPERIMENT_MGENES_PATH=${ATLAS_DATA_SCXA_DEST}/magetab/${EXP_ID} \
  ./load_db_scxa_marker_genes.sh
done

./reindex_tables.sh
