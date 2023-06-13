FROM ubuntu:jammy

# Update package lists and install lftp
RUN apt-get update \
    && apt-get install -y --no-install-recommends lftp

# Clean up APT cache and temporary files
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Copy the auxiliary JSON files to the parent of the magetab directory
COPY atlas-data/exp/* /atlas-data/exp/

ENTRYPOINT ["/bin/bash", "-c"]
