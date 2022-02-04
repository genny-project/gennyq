#!/bin/bash
host="${1:-http://localhost}"
gennyhost="${2:-http://alyson.genny.life}"
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
export GENNY_KOGITO_SERVICE_URL=${host}:9580
export GENNY_KOGITO_DATAINDEX_HTTP_URL=${gennyhost}:8582
export GENNY_KOGITO_DATAINDEX_WS_URL=ws://${rawhost}:8582
export GENNY_INFINISPAN_URL=${rawhost}:11222
export GENNY_INFINISPAN_CLIENT_AUTH_PASSWORD=password
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
export QUARKUS_OIDC_AUTH-SERVER-URL="${GENNY_KEYCLOAK_URL}"
export QUARKUS_OIDC_CLIENT-ID=${GENNY_CLIENT_ID}
echo "infinispan url $GENNY_INFINISPAN_URL"
echo "infinispan username $GENNY_INFINISPAN_USERNAME"
echo "infinispan password $GENNY_INFINISPAN_PASSWORD"
echo "genny kafka url $GENNY_KAFKA_URL"
echo "genny data-index http $GENNY_KOGITO_DATAINDEX_HTTP_URL"
echo "genny data-index ws $GENNY_KOGITO_DATAINDEX_WS_URL"
echo "jobservice $GENNY_KOGITO_JOBSERVICE_URL"
echo "kogito service $GENNY_KOGITO_SERVICE_URL"
./mvnw  quarkus:dev -Ddebug=5580 -Dquarkus.http.port=9580 -DskipTests=true

