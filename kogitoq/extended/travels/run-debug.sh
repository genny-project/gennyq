#!/bin/bash
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280
export GENNY_KAFKA_URL=alyson2.genny.life:9092
export GENNY_CLIENT_ID=kogito-console-quarkus
export GENNY_REALM=internmatch
export GENNY_KOGITO_SERVICE_URL=http://alyson2.genny.life:8580
echo $GENNY_KEYCLOAK_URL
echo $GENNY_SERVICE_USERNAME
echo $GENNY_SERVICE_PASSWORD
echo $GENNY_CLIENT_ID
echo $GENNY_CLIENT_SECRET
echo $GENNY_REALM
export GENNY_INFINISPAN_URL=alyson2.genny.life:11222
echo $GENNY_INFINISPAN_URL
echo $GENNY_INFINISPAN_USERNAME
echo $GENNY_INFINISPAN_PASSWORD
echo $GENNY_KAFKA_URL
echo $GENNY_KOGITO_DATAINDEX_HTTP_URL
echo $GENNY_KOGITO_DATAINDEX_WS_URL
echo $GENNY_KOGITO_JOBSERVICE_URL
echo $GENNY_KOGITO_SERVICE_URL
./mvnw  quarkus:dev -Ddebug=5580 -Dquarkus.http.port=8580 -DskipTests=true

