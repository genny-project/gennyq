#!/bin/bash

project=$1

if [ "$#" -eq 1 ]; then
	./mvnw clean package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$1 -T 1C
	exit 0
fi

echo "Building all sub-projects!"
./mvnw clean package -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -T 1C
