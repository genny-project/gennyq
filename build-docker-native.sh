#!/bin/bash

VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

buildNativeImage()
{
	PROJECT=$1
	VERSION=$2

	PATH=$(find . -type d -name $PROJECT | head -n 1)

	docker build -f ${PATH}/src/main/docker/Dockerfile.native -t gennyproject/${PROJECT}:${VERSION}-native $PATH
}

if [ "$#" -eq 1 ]; then
	./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true -Dstyle.color=always -pl :$1
	docker tag gennyproject/${1}:${VERSION} gennyproject/${1}:${VERSION}-native
	# buildNativeImage $1 $VERSION
	exit 0
fi

./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true -Dstyle.color=always

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages
do
    echo "Tagging $project"
	# buildNativeImage $project $VERSION
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:${VERSION}-native
done

