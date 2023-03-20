FROM ubuntu:jammy

# Remove references to broken/outdated packages; otherwise we can get 404s:
# E: Failed to fetch http://archive.ubuntu.com/ubuntu/pool/main/r/rsync/rsync_3.2.3-8ubuntu3.1_amd64.deb  404  Not Found [IP: 185.125.190.39 80
RUN rm -rf /var/lib/apt/lists/*
RUN apt -y -qq update
RUN apt -y -qq install git openjdk-11-jdk rsync

RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git /root/atlas-web-single-cell
WORKDIR /root/atlas-web-single-cell

VOLUME /gradle-wrapper-dists
VOLUME /gradle-ro-dep-cache
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
