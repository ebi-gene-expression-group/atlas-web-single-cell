FROM ubuntu:jammy

RUN apt -y -qq update
RUN apt -y -qq install lftp

VOLUME /atlas-data/bioentity_properties /atlas-data/scxa /atlas-data/scxa-expdesign /webapp-properties
RUN mkdir -p /atlas-data/scxa/magetab

COPY atlas-data/scxa/* /atlas-data/scxa/
COPY webapp-properties/* /webapp-properties/

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
