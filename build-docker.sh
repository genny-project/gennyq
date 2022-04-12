#!/bin/bash

VERSION=$( mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

mvn clean package -Dquarkus.container-image.build=true -DskipTests=true -Dstyle.color=always

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages
do
    echo "Tagging $project"
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:latest
done

