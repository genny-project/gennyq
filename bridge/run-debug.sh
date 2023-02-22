#!/bin/bash
host="${1:-http://alyson2.genny.life}"
gennyhost="${2:-http://alyson.genny.life}"
port="${3:-10020}"
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
#export GENNY_CLIENT_ID=kogito-console-quarkus
export GENNY_CLIENT_ID=backend
export GENNY_REALM=internmatch
export GENNY_KOGITO_SERVICE_URL=${host}:${port}
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
export INFINISPAN_PASSWORD=password
export QUARKUS_OIDC_AUTH_SERVER_URL="${GENNY_KEYCLOAK_URL:https\://keycloak.gada.io/auth}/realms${GENNY_REALM}"
export QUARKUS_OIDC_CLIENT_ID=${GENNY_CLIENT_ID}
export QUARKUS_INFINISPAN_CLIENT_CLIENT_INTELLIGENCE=BASIC
export INFINISPAN_CLIENT_HOTROD_SERVER_LIST=alyson.genny.life:11222
export INFINISPAN_CLIENT_HOTROD_CLIENT_INTELLIGENCE=BASIC
export FYODOR_SERVICE_API=http://alyson.genny.life:10070

#export GENNY_MYSQL_FULL_URL=alyson.genny.life:3310/gennydb?zeroDateTimeBehavior=convertToNull&serverTimezone=UTC
export FULL_MYSQL_URL=jdbc:mysql://alyson.genny.life:3310/gennydb?allowPublicKeyRetrieval=true&syscharacterEncoding=utf8mb4&useSSL=FALSE&serverTimezone=UTC&rewriteBatchedStatements=true
export MYSQL_USER=genny
export MYSQL_PASSWORD=password
export GENNY_MYSQL_USERNAME=genny
export GENNY_MYSQL_PASSWORD=password
export GENNY_MYSQL_URL=alyson.genny.life
export GENNY_MYSQL_PORT=3310
export GENNY_MYSQL_DB=gennydb
export ENV_KEYCLOAK_REDIRECTURI=${GENNY_KEYCLOAK_URL:https\://keycloak.gada.io/auth}
export RULESSERVICE_URL=http://wildfly-rulesservice
export MEDIA_PROXY_URL=https://alyson.genny.life/web/public

echo "infinispan url $GENNY_INFINISPAN_URL"
echo "infinispan username $GENNY_INFINISPAN_CLIENT_AUTH_USERNAME"
echo "infinispan password $GENNY_INFINISPAN_CLIENT_AUTH_PASSWORD"
echo "genny kafka url $GENNY_KAFKA_URL"
echo "genny data-index http $GENNY_KOGITO_DATAINDEX_HTTP_URL"
echo "genny data-index ws $GENNY_KOGITO_DATAINDEX_WS_URL"
echo "jobservice $GENNY_KOGITO_JOBSERVICE_URL"
echo "kogito service $GENNY_KOGITO_SERVICE_URL"

./mvnw clean  quarkus:dev -Ddebug=5480 -Dquarkus.http.port=${port} -DskipTests=true -Dinfinispan.client.hotrod.server_list=10.123.123.123:11222 -Dinfinispan.client.hotrod.client_intelligence=BASIC
