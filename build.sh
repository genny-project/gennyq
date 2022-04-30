#!/bin/bash

project=$1

if [ "$#" -eq 1 ]; then
	./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$1
	exit 0
fi

echo "Building all sub-projects!"
./mvnw clean install -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always 
