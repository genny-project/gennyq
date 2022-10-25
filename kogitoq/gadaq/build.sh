#!/bin/bash
rm -Rf src/main/proto/*
#./mvnw clean install -DskipTests=true -Dcheckstyle.skip
./mvnw clean install -DskipTests=false -Dcheckstyle.skip
