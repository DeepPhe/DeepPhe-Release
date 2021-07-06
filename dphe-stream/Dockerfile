FROM maven:3.8.1-jdk-8

WORKDIR /usr/src/app

# Copy everything else from host to image
COPY . .

# Unzip the source code zip
RUN unzip v0.1.0-cr-release.zip

# Install local dependencies
WORKDIR /usr/src/app/v0.1.0-cr-release/dphe-onto-db
RUN mvn clean install -DskipTests

WORKDIR /usr/src/app/v0.1.0-cr-release/dphe-neo4j
RUN mvn clean install -DskipTests

WORKDIR /usr/src/app/v0.1.0-cr-release/dphe-core
RUN mvn clean install -DskipTests

WORKDIR /usr/src/app/v0.1.0-cr-release/dphe-stream
RUN mvn clean install -DskipTests

# Build the REST API application jar
WORKDIR /usr/src/app/v0.1.0-cr-release/dphe-stream-rest

# Remove the default application.properties 
# Remove the default deepphe.properties
# Replace with new application.properties(use port 8080 instead of 9000)
# Replace with new deepphe.properties(use a different auth token)
# Maven build
RUN rm -rf src/main/resources/application.properties && \
    rm -rf src/main/resources/deepphe.properties && \
    mv /usr/src/app/application.properties src/main/resources/application.properties && \
    mv /usr/src/app/deepphe.properties src/main/resources/deepphe.properties && \
    mvn clean package -DskipTests

# Install gosu for de-elevating root to deepphe user
RUN curl -o /usr/local/bin/gosu -SL "https://github.com/tianon/gosu/releases/download/1.12/gosu-amd64" && \
    curl -o /usr/local/bin/gosu.asc -SL "https://github.com/tianon/gosu/releases/download/1.12/gosu-amd64.asc" && \
    rm /usr/local/bin/gosu.asc && \
    chmod +x /usr/local/bin/gosu

# The EXPOSE instruction informs Docker that the container listens on the specified network ports at runtime.
# EXPOSE does not make the ports of the container accessible to the host.
# Here use 8080 because in application.properties we use port 8080
EXPOSE 8080

# Set an entrypoint
RUN cp /usr/src/app/entrypoint.sh /usr/local/bin/entrypoint.sh && \
    chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

# Start the rest api server on port 8080 running as non-root
# Note: since the current work dir is /usr/src/app/v0.1.0-cr-release/dphe-stream-rest, the logs will be generated here too
# Thus we use /usr/src/app/v0.1.0-cr-release/dphe-stream-rest/logs for the volume mount in docker-compose.yml
CMD ["java", "-jar","-Xmx6G","/usr/src/app/v0.1.0-cr-release/dphe-stream-rest/target/deepphe-stream-rest-0.1.0-cr.jar"]
