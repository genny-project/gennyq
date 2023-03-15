#!/bin/bash
export GENNY_SHOW_VALUES="TRUE"
export GENNY_KAFKA_URL=alyson.genny.life:9092
export GENNY_KEYCLOAK_REALM=${GENNY_KEYCLOAK_REALM:internmatch}
export GENNY_CLIENT_ID=backend
export GENNY_CLIENT_SECRET=${GENNY_CLIENT_SECRET}
export GENNY_SERVICE_USERNAME=${GENNY_SERVICE_USERNAME}
export GENNY_SERVICE_PASSWORD=${GENNY_SERVICE_PASSWORD}
export GENNY_MYSQL_FULL_URL=alyson.genny.life:3310/gennydb?zeroDateTimeBehavior=convertToNull&serverTimezone=UTC
export MYSQL_USER=genny
export MYSQL_PASSWORD=password
export INFINISPAN_URL=alyson.genny.life:11222
export INFINISPAN_USERNAME=genny
export INFINISPAN_PASSWORD=password
export PROJECT_REALM=${GENNY_KEYCLOAK_REALM:internmatch}
export PROJECT_URL=https://internmatch.genny.life
export GOOGLE_SVC_ACC_PATH=~/.genny/sheets.googleapis.com-java-quickstart/token-secret-service-account.json
 ./mvnw clean quarkus:dev -Ddebug=5106
