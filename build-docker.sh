#!/bin/bash
VERSION=$( mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

# Usage: ./build-docker.sh [project] 

if [ "$#" -eq 1 ]; then
	./mvnw clean  package -Dquarkus.container-image.build=true -DskipTests=true -Dstyle.color=always -pl :$1
	docker tag gennyproject/${1}:${VERSION} gennyproject/${1}:latest
	exit 0
fi

./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true -Dstyle.color=always

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages
do
    echo "Tagging $project"
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:latest
done

