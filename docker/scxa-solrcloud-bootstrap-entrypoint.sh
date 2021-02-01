#!/usr/bin/env bash

/index-gxa/bin/create-bioentities-collection.sh
/index-gxa/bin/create-bioentities-schema.sh

/index-scxa/bin/create-scxa-gene2experiment-config-set.sh
/index-scxa/bin/create-scxa-gene2experiment-collection.sh
/index-scxa/bin/create-scxa-gene2experiment-schema.sh

cd /index-scxa
git checkout 0.5.0
/index-scxa/bin/create-scxa-analytics-config-set.sh
/index-scxa/bin/create-scxa-analytics-collection.sh
/index-scxa/bin/create-scxa-analytics-biosolr-lib.sh
/index-scxa/bin/create-scxa-analytics-schema.sh

git checkout 0.4.0
/index-scxa/bin/create-scxa-analytics-config-set.sh
/index-scxa/bin/create-scxa-analytics-collection.sh
# We only want the most recent version of BioSolr
# /index-scxa/bin/create-scxa-analytics-biosolr-lib.sh
/index-scxa/bin/create-scxa-analytics-schema.sh

git checkout 0.3.0
/index-scxa/bin/create-scxa-analytics-config-set.sh
/index-scxa/bin/create-scxa-analytics-collection.sh
# We only want the most recent version of BioSolr
# /index-scxa/bin/create-scxa-analytics-biosolr-lib.sh
/index-scxa/bin/create-scxa-analytics-schema.sh

# Give Solr a bit of time to finish setting up the collections.
# Otherwise we could issue a restore request to a (yet) non-existing collection!
sleep 10s

# Restore all collections
# Env variables come from Dockerfile

for SOLR_HOST in $SOLR_HOSTS
do
  for SOLR_COLLECTION in $SOLR_COLLECTIONS
  do
    curl "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restore&location=/var/backups/solr&name=${SOLR_COLLECTION}"

    # Pattern enclosed in (?<=) is zero-width look-behind and (?=) is zero-width look-ahead, we match everything in between
    STATUS=`curl -s "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restorestatus" | grep -oP '(?<="status":").*(?=")'`

    WAIT_SECONDS=1
    # We wait until "status" field is "success"
    while [ "${STATUS}" != "success" ]
    do
      echo "Restoring ${SOLR_COLLECTION} at ${SOLR_HOST}: ${STATUS}"
      sleep ${WAIT_SECONDS}s
      STATUS=`curl -s "http://${SOLR_HOST}/solr/${SOLR_COLLECTION}/replication?command=restorestatus" | grep -oP '(?<="status":").*(?=")'`
      WAIT_SECONDS=$(( WAIT_SECONDS + WAIT_SECONDS ))
    done
  done
done

# Set aliases
curl "http://${SOLR_HOST}/solr/admin/collections?action=CREATEALIAS&name=bioentities&collections=bioentities-v1"
curl "http://${SOLR_HOST}/solr/admin/collections?action=CREATEALIAS&name=scxa-analytics&collections=scxa-analytics-v1"
curl "http://${SOLR_HOST}/solr/admin/collections?action=CREATEALIAS&name=scxa-gene2experiment&collections=scxa-gene2experiment-v1"
printf "The following aliases have been set:\n"
curl "http://${SOLR_HOST}:8983/solr/admin/collections?action=LISTALIASES"
