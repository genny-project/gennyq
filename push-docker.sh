#!/bin/bash
VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
VERSION=${1-$VERSION}
echo "Project Version: $VERSION"

for project in gadaq bridge fyodor dropkick lauchy messages shleemy
do
    echo "Pushing $project"
	docker push gennyproject/${project}:${VERSION} 
	docker push gennyproject/${project}:latest
done
