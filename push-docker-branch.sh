#!/bin/bash
TAG=$1

for project in gadaq bridge fyodor dropkick lauchy messages shleemy
do
    echo "Pushing $project"
	docker push gennyproject/${project}:${TAG} 
	docker push gennyproject/${project}:latest
done
