#!/bin/bash
VERSION=$( ./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Project Version: $VERSION"

for project in gadaq adi bridge fyodor dropkick lauchy messages shleemy
do
    echo "Pushing $project"
	docker push gennyproject/${project}:${VERSION} 
	docker push gennyproject/${project}:latest
done
