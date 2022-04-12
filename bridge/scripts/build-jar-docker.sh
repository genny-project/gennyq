#!/bin/bash

source ../.env

cd ../

./mvnw clean package -Dquarkus.container-image.build=true

cd -

