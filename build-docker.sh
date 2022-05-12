#!/bin/bash
VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

# Usage: ./build-docker.sh [project] 

if [ "$#" -ge 1 ]; then
	projects=( $@ )
else
	projects=( qwandaq serviceq gadaq bridge fyodor dropkick lauchy messages shleemy )
fi

# iterate projects
for project in "${projects[@]}"
do
    echo "Building $project"

	# remove target dir
	rm -rf $project/target
	rm -rf kogito/$project/target

	# perform clean install
	./mvnw clean install -Dquarkus.container-image.build=true -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$project

	# tag the docker container
    echo "Tagging $project"
	docker tag gennyproject/${project}:${VERSION} gennyproject/${project}:latest
done

