#!/bin/bash

project=$1

if [ "$#" -eq 1 ]; then
	./mvnw package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$1 -T 1C -o -am
	exit 0
fi

echo "Building all sub-projects!"
./mvnw package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -T 1C -o -am
