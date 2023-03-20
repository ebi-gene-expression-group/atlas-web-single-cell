FROM ubuntu:jammy

# Remove references to broken/outdated packages; otherwise we can get 404s:
# E: Failed to fetch http://archive.ubuntu.com/ubuntu/pool/main/r/rsync/rsync_3.2.3-8ubuntu3.1_amd64.deb  404  Not Found [IP: 185.125.190.39 80
RUN rm -rf /var/lib/apt/lists/*
RUN apt -y -qq update
RUN apt -y -qq install lftp

VOLUME /atlas-data/bioentity_properties /atlas-data/scxa /atlas-data/scxa-expdesign /webapp-properties
RUN mkdir -p /atlas-data/scxa/magetab

COPY atlas-data/scxa/* /atlas-data/scxa/
COPY webapp-properties/* /webapp-properties/

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
