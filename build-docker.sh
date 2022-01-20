#!/bin/bash
PROJECT_VERSION=$( mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo $PROJECT_VERSION

mvn clean package -Dquarkus.container-image.build=true -DskipTests=true
docker tag gennyproject/kogitoq-visas:${PROJECT_VERSION} gennyproject/kogitoq-visas:latest
docker tag gennyproject/kogitoq-travels:${PROJECT_VERSION} gennyproject/kogitoq-travels:latest
docker push gennyproject/kogitoq-visas:latest
docker push gennyproject/kogitoq-travels:latest


