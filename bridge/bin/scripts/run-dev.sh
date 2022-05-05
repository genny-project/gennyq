#!/bin/bash

source ../.env
cd ../
./mvnw quarkus:dev -Dquarkus.http.host=0.0.0.0 -DskipTests=true

cd -
