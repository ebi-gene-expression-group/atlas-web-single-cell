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
curl "http://${SOLR_HOST}/solr/admin/collections?action=LISTALIASES"

printf "\nPLEASE READ!\n"
printf "Suggesters haven’t been built because it’s very likely to get a java.net.SocketTimeoutException due\n"
printf "to the size of the bioentities collection. Raising the timeout in Jetty could mask other errors down\n"
printf "the line, and ignoring the exception doesn’t guarantee the suggester to be fully built since it still\n"
printf "takes a few extra minutes: the exception is thrown before the process has completed.\n"
printf "The best option is to manually build and supervise this step.\n"
printf "\n"
printf "On one terminal session run the following command (don’t worry if the request returns a 500 error):\n"
printf "docker exec -i scxa-solrcloud-1 curl 'http://localhost:8983/solr/bioentities-v1/suggest?suggest.build=true&suggest.dictionary=propertySuggesterNoHighlight'\n"
printf "\n"
printf "On another terminal, monitor the size of the suggester directory size:\n"
printf "docker exec -it scxa-solrcloud-1 bash -c 'watch du -sc server/solr/bioentities-v1*/data/*'\n"
printf "\n"
printf "The suggester will be built when the propertySuggester directory size stabilises.\n"
printf "Run the above procedure for each of your SolrCloud containers and both suggesters\n"
printf "(propertySuggesterNoHighlight bioentitySuggester).\n"
