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

if [ "$#" -ge 1 ]; then
	dependencies=( )
	projects=( $@ )
else
	dependencies=( qwandaq serviceq )
	projects=( gadaq bridge fyodor dropkick lauchy messages )
fi

# iterate dependencies
for dependency in "${dependencies[@]}"
do
    echo "Building $dependency"

	# remove target dir
	rm -rf $project/target
	rm -rf kogitoq/$project/target

	# perform clean install
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$dependency
done

# iterate projects
for project in "${projects[@]}"
do
    echo "Building $project"

	# remove target dir
	rm -rf $project/target
	rm -rf kogitoq/$project/target

	# copy each project with the latest docker
	cp -f docker/* $project/src/main/docker/ 
	cp -f docker/* kogito/$project/src/main/docker/ 

	# perform clean install with docker build
	./mvnw clean package -Pnative -Dquarkus.native.remote-container-build=true -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:21.3-java11 -DskipTests=true -Dstyle.color=always -pl :$1

	# tag the docker container
    echo "Tagging $project"
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:${VERSION}-native
done

