FROM ubuntu:jammy

RUN apt -y -qq update
RUN apt -y -qq install git openjdk-11-jdk rsync

RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git /root/atlas-web-single-cell
WORKDIR /root/atlas-web-single-cell

VOLUME /gradle-ro-dep-cache
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8"

COPY docker-entrypoint.sh /
CMD ["/docker-entrypoint.sh"]
