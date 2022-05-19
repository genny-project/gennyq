#!/bin/bash

cd ../
./mvnw package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true -DskipTests

cd -
