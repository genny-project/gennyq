#!/bin/bash

if [ -z "$1" ]; then
	echo "Must supply a version as input"
	exit 1;
fi

VERSION=${1}
echo "Pushin Version: $VERSION"

for project in gadaq bridge fyodor dropkick lauchy messages shleemy
do
    echo "Pushing $project"
	docker push gennyproject/${project}:${VERSION} 
	docker push gennyproject/${project}:latest
done
