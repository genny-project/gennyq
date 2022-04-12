#!/bin/bash

if [ "$#" -eq 1 ]; then
	./mvnw clean package -DskipTests=true -Dcheckstyle.skip -rf :$1
	exit 1
fi

echo "Building all sub-projects!"
./mvnw clean package -DskipTests=true -Dcheckstyle.skip 
