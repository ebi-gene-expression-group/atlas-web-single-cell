FROM openjdk:11
RUN apt -y -qq update
RUN apt install -y -qq jq rsync

WORKDIR /root
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-bioentities.git
RUN git clone --depth 1 --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
RUN git clone --depth 1 https://github.com/ebi-gene-expression-group/index-scxa.git

#RUN mkdir /root/gxa \
# RUN echo [] > /root/gxa/species-properties.json
ENV SPECIES homo_sapiens mus_musculus arabidopsis_thaliana solanum_lycopersicum glycine_max
ENV EXP_IDS E-CURD-4 E-EHCA-2 E-GEOD-71585 E-GEOD-81547 E-GEOD-99058 E-MTAB-5061 E-ENAD-53

ENV SOLR_NUM_SHARDS 2
ENV SOLR_HOST scxa-solrcloud-0:8983

ENV SOLR_COLLECTION_BIOENTITIES bioentities
ENV SOLR_COLLECTION_BIOENTITIES_SCHEMA_VERSION 1

ENV SOLR_COLLECTION_ANALYTICS scxa-analytics
ENV SOLR_COLLECTION_ANALYTICS_SCHEMA_VERSION 7
ENV BIOSOLR_VERSION 2.0.0
ENV BIOSOLR_JAR_PATH /root/index-scxa/lib/solr-ontology-update-processor-2.0.0.jar
ENV SIGNING_PRIVATE_KEY /root/secrets/scxa-solrcloud.pem
ENV SCXA_ONTOLOGY file:///var/solr/data/userfiles/scatlas.owl

COPY ./docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
