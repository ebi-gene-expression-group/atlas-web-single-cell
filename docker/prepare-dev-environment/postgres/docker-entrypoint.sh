#!/usr/bin/env bash

cd /root/atlas-web-single-cell

# Creating the experiments is required because certain scripts below use experiment.accession as a foreign key, and
# we get the experiment designs as a desired side benefit. They are necessary for ITs and for running the web app.
./gradlew \
-PexperimentFilesLocation=/atlas-data/scxa \
-PexperimentDesignLocation=/atlas-data/scxa-expdesign \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
:cli:bootRun --args="create-update-experiment -e $(echo ${EXP_IDS} | sed -e "s/ /,/g")"

# The last commit before v19 was introduced...
if [ ${SCHEMA_VERSION} = 18 ] ; then
  cd /root/db-scxa
  git checkout 1236753d3d799effa4d24fa9bdfb9292c66309ab
fi

cd /root/db-scxa/bin
export PATH=.:${PATH}
export dbConnection=postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD}@${POSTGRES_HOST}:5432/${POSTGRES_DB}

for EXP_ID in ${EXP_IDS}
do
  EXPRESSION_TYPE=aggregated_filtered_normalised_counts \
  EXP_ID=${EXP_ID} \
  EXPERIMENT_MATRICES_PATH=/atlas-data/scxa/magetab/${EXP_ID} ./load_db_scxa_analytics.sh

  if [ ${SCHEMA_VERSION} = 18 ]; then
    EXP_ID=${EXP_ID} EXPERIMENT_DIMRED_PATH=/atlas-data/scxa/magetab/${EXP_ID} ./load_db_scxa_dimred.sh
  else
    # Copied from db-scxa/tests/test-dimred-load.sh
    ls /atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.tsne*.tsv | while read -r l; do
      DIMRED_TYPE=tsne
      DIMRED_FILE_PATH=$l
      DIMRED_PARAM_JSON='[{"perplexity": '$(echo "$l" | sed 's/.*[^0-9]\([0-9]*\).tsv/\1/g')'}]'
      load_db_scxa_dimred.sh ${dbConnection} ${EXP_ID} ${DIMRED_TYPE} ${DIMRED_FILE_PATH} "${DIMRED_PARAM_JSON}"
    done

    ls /atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.umap*.tsv | while read -r l; do
      DIMRED_TYPE=umap
      DIMRED_FILE_PATH=$l
      DIMRED_PARAM_JSON='[{"n_neighbors": '$(echo "$l" | sed 's/.*[^0-9]\([0-9]*\).tsv/\1/g')'}]'
      load_db_scxa_dimred.sh ${dbConnection} ${EXP_ID} ${DIMRED_TYPE} ${DIMRED_FILE_PATH} "${DIMRED_PARAM_JSON}"
    done
  fi

  EXP_ID=${EXP_ID} \
  EXPERIMENT_CLUSTERS_FILE=/atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.clusters.tsv \
  CONDENSED_SDRF_TSV=/atlas-data/scxa/magetab/${EXP_ID}/${EXP_ID}.condensed-sdrf.tsv \
  ./load_db_scxa_cell_clusters.sh

  EXP_ID=${EXP_ID} \
  CLUSTERS_FORMAT=SCANPY \
  EXPERIMENT_MGENES_PATH=/atlas-data/scxa/magetab/${EXP_ID} \
  ./load_db_scxa_marker_genes.sh
done

./reindex_tables.sh
