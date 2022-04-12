#!/bin/bash

VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

if [ "$#" -eq 1 ]; then
	./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true -pl :$1
	# docker tag gennyproject/${1}:${VERSION} gennyproject/${1}:${VERSION}-native
	exit 0
fi

./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages
do
    echo "Tagging $project"
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:latest
done









# export JAVA_HOME=$GRAALVM_HOME
# export PATH=$JAVA_HOME/bin:$PATH
# java -version


# docker build -f src/main/docker/Dockerfile.native -t  gennyproject/fyodor:${VERSION_TO_BUILD}-native .


