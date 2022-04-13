#!/bin/bash

project=$1

if [ "$#" -eq 1 ]; then
	./mvnw package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$1
	exit 0
fi

echo "Building all sub-projects!"
./mvnw package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always 
