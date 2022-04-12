#!/bin/bash

if [ "$#" -eq 1 ]; then
	./mvnw clean package -Pnative -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always -pl :$1
	exit 0
fi

./mvnw clean package -Pnative -DskipTests=true -Dcheckstyle.skip -Dstyle.color=always

