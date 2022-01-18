#!/bin/bash
project=`echo "${PWD##*/}" | tr '[:upper:]' '[:lower:]'`
project=visas
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
#version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
  version=1.14.0.Final
else
  version="${1}"
fi

echo "Project version: ${version}"
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=http://alyson.genny.life:8280
export GENNY_KAFKA_URL=alyson.genny.life:9092
export GENNY_CLIENT_ID=kogito-console-quarkus
export GENNY_REALM=internmatch
export GENNY_KOGITO_SERVICE_URL=http://alyson2.genny.life:8580
echo $GENNY_KEYCLOAK_URL
echo $GENNY_SERVICE_USERNAME
echo $GENNY_SERVICE_PASSWORD
echo $GENNY_CLIENT_ID
echo $GENNY_CLIENT_SECRET
echo $GENNY_REALM
export GENNY_INFINISPAN_URL=alyson.genny.life:11222
echo $GENNY_INFINISPAN_URL
echo $GENNY_INFINISPAN_USERNAME
echo $GENNY_INFINISPAN_PASSWORD
echo $GENNY_KAFKA_URL
echo $GENNY_KOGITO_DATAINDEX_HTTP_URL
echo $GENNY_KOGITO_DATAINDEX_WS_URL
echo $GENNY_KOGITO_JOBSERVICE_URL
echo $GENNY_KOGITO_SERVICE_URL
./target/${project}-runner  -Dquarkus.http.port=8579
