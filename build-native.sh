#!/bin/bash

if [ "$#" -eq 1 ]; then
	./mvnw clean package -Pnative -DskipTests=true -Dcheckstyle.skip -pl :$1
	exit 1
fi

./mvnw clean package -Pnative -DskipTests=true -Dcheckstyle.skip 

