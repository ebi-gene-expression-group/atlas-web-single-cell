FROM ubuntu:latest
RUN apt -y -qq update
RUN apt install -y -qq lftp bash

RUN mkdir -p /scxa-data/magetab /bioentity_properties
COPY release-metadata.json /scxa-data
COPY species-properties.json /scxa-data
VOLUME /scxa-data /bioentity_properties

ENV EXP_IDS E-CURD-4 E-EHCA-2 E-GEOD-71585 E-GEOD-81547 E-GEOD-99058 E-MTAB-5061 E-ENAD-53
COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
