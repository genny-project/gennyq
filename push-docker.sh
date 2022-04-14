#!/bin/bash
VERSION=$( mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

# Usage: ./build-docker.sh [project] 

for project in kogitoq-travels kogitoq-visas adi bridge fyodor dropkick lauchy messages shleemy
do
    echo "Pushing $project"
	docker push gennyproject/${project}:${VERSION} 
        docker push gennyproject/${project}:latest
done
