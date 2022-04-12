#!/bin/bash

VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

buildNativeImage()
{
	PROJECT=$1
	VERSION=$2

	PATH=$(find . -type d -name $PROJECT | head -n 1)

	docker build -f ${PATH}/src/main/docker/Dockerfile.native -t  gennyproject/${PROJECT}:${VERSION}-native $PATH
}

if [ "$#" -eq 1 ]; then
	buildNativeImage $1 $VERSION
	./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true -pl :$1
	exit 0
fi

./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages
do
    echo "Tagging $project"
	buildNativeImage $project $VERSION
done

