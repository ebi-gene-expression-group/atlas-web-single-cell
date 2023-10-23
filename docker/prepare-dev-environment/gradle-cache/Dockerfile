FROM ubuntu:jammy

# Update package list and install necessary tools
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    git openjdk-11-jdk rsync

# Clean up APT cache and temporary files
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Clone the necessary repository
RUN git clone --recurse-submodules --depth 1 https://github.com/ebi-gene-expression-group/atlas-web-single-cell.git /root/atlas-web-single-cell

ENTRYPOINT ["/bin/bash", "-c"]
