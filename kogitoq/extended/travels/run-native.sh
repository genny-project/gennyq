#!/bin/bash
project=`echo "${PWD##*/}" | tr '[:upper:]' '[:lower:]'`
project=travels
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
#version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
  version=1.18.0.Final
else
  version="${1}"
fi

echo "Project version: ${version}"
host="${1:-http://alyson2.genny.life}"
gennyhost="${2:-http://alyson.genny.life}"
port="${3:-9580}"
parser=`echo "$gennyhost" | awk -F/ '{print $3}' `
echo "Parser = ${parser}"
rawhost=${parser}
echo "host (this) = ${host}"
echo "genny host (target system) = ${gennyhost}"
echo "raw host (target system) = ${rawhost}"


export GENNY_SHOW_VALUES="TRUE"
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=${gennyhost}:8280
export GENNY_KAFKA_URL=${gennyhost}:9092
export GENNY_CLIENT_ID=kogito-console-quarkus
export GENNY_REALM=internmatch
export GENNY_KOGITO_SERVICE_URL=${host}:${port}
export GENNY_KOGITO_DATAINDEX_HTTP_URL=${gennyhost}:8582
export GENNY_KOGITO_DATAINDEX_WS_URL=ws://${rawhost}:8582
export GENNY_INFINISPAN_URL=${rawhost}:11222
export GENNY_INFINISPAN_CLIENT_AUTH_PASSWORD=password
export INFINISPAN_PASSWORD=password
export GENNY_INFINISPAN_CLIENT_AUTH_USERNAME=genny
export GENNY_INFINISPAN_CLIENT_SERVER_LIST=${gennyhost}:11222
export GENNY_INFINISPAN_CLIENT_USE_AUTH="true"
export KOGITO_DATAINDEX_HTTP_URL=${gennyhost}:8582
export KOGITO_DATAINDEX_WS_URL=ws://${rawhost}:8582
export GENNY_KOGITO_JOBSERVICE_URL=${gennyhost}:8581
export QUARKUS_INFINISPAN_CLIENT_AUTH_PASSWORD=password
export QUARKUS_INFINISPAN_CLIENT_AUTH_REALM=default
export QUARKUS_INFINISPAN_CLIENT_AUTH_USERNAME=genny
export QUARKUS_INFINISPAN_CLIENT_SASL_MECHANISM=DIGEST-MD5
export QUARKUS_INFINISPAN_CLIENT_SERVER_LIST=${rawhost}:11222
export QUARKUS_INFINISPAN_CLIENT_USE_AUTH="true"
export GENNY_MYSQL_FULL_URL=alyson.genny.life:3310/gennydb?zeroDateTimeBehavior=convertToNull&serverTimezone=UTC
export FULL_MYSQL_URL=jdbc:mysql://alyson.genny.life:3310/gennydb?allowPublicKeyRetrieval=true&syscharacterEncoding=utf8mb4&useSSL=FALSE&serverTimezone=UTC&rewriteBatchedStatements=true
export MYSQL_USER=genny
export MYSQL_PASSWORD=password
export FYODOR_SERVICE_API=http://alyson.genny.life:4242
echo "infinispan url $GENNY_INFINISPAN_URL"
echo "infinispan username $GENNY_INFINISPAN_CLIENT_AUTH_USERNAME"
echo "infinispan password $GENNY_INFINISPAN_CLIENT_AUTH_PASSWORD"
echo "genny kafka url $GENNY_KAFKA_URL"
echo "genny data-index http $GENNY_KOGITO_DATAINDEX_HTTP_URL"
echo "genny data-index ws $GENNY_KOGITO_DATAINDEX_WS_URL"
echo "jobservice $GENNY_KOGITO_JOBSERVICE_URL"
echo "kogito service $GENNY_KOGITO_SERVICE_URL"
./target/${project}-runner  -Dquarkus.http.port=${port}
