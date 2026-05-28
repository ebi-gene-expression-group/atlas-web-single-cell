# Docker image for atlas-web-single-cell, based on the same Tomcat image
# used by the Kubernetes Helm chart.
#
# The Jenkins pipeline assembles the WAR under webapps/gxa#sc.war
# (stage "Assemble WAR file" in Jenkinsfile). This image simply
# bakes that WAR into Tomcat; all runtime configuration (JDBC,
# Solr, Tomcat users/manager, etc.) remains managed by K8s
# ConfigMaps/Secrets and Helm values.
#
# The WAR filename gxa#sc.war maps to Tomcat context path /gxa/sc.

FROM tomcat:8-jdk11

# Application user/group matching the Kubernetes podSecurityContext
ARG APP_USER=docker
ARG APP_UID=2921
ARG APP_GID=1146

ENV USER=${APP_USER} UID=${APP_UID} GID=${APP_GID}

RUN groupadd -g "${APP_GID}" "${APP_USER}" \
 && useradd -u "${APP_UID}" -g "${APP_GID}" \
      -d /usr/local/tomcat -s /bin/bash "${APP_USER}" \
 && chown -R "${APP_UID}:${APP_GID}" /usr/local/tomcat

# Remove default webapps so only our application is deployed
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the application WAR into Tomcat. The filename gxa#sc.war
# maps to context path /gxa/sc in Tomcat.
COPY "webapps/gxa#sc.war" /usr/local/tomcat/webapps/

# Expose the default HTTP port. Debug and other ports are controlled
# at the Kubernetes level via Helm values (tomcat.debug.*).
EXPOSE 8080

# Run as the non-root application user by default. The Helm chart
# can still override securityContext if needed, but by default this
# image will use the same UID/GID as configured there.
USER ${APP_UID}:${APP_GID}

# Default command; can be overridden by the Helm chart when JPDA
# debugging is enabled.
CMD ["catalina.sh", "run"]
