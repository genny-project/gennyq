#!/bin/bash

if [ "$#" -eq 1 ]; then
	./mvnw clean package -DskipTests=true -Dcheckstyle.skip -pl :$1
	exit 0
fi

echo "Building all sub-projects!"
./mvnw clean package -DskipTests=true -Dcheckstyle.skip 
