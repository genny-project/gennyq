#!/bin/bash
host="${1:-http://alyson2.genny.life}"
gennyhost="${2:-http://alyson.genny.life}"
port=8080
parser=`echo "$gennyhost" | awk -F/ '{print $3}' `
echo "Parser = ${parser}"
rawhost=${parser}
echo "host (this) = ${host}"
echo "genny host (target system) = ${gennyhost}"
echo "raw host (target system) = ${rawhost}"


export GENNY_SHOW_VALUES="TRUE"
export GENNY_KAFKA_URL=alyson.genny.life:9092
export GENNY_REALM=internmatch
export GENNY_CLIENT_ID=backend
export GENNY_CLIENT_SECRET=${GENNY_CLIENT_SECRET}
export GENNY_SERVICE_USERNAME=${GENNY_SERVICE_USERNAME}
export GENNY_SERVICE_PASSWORD=${GENNY_SERVICE_PASSWORD}
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_MYSQL_FULL_URL=alyson.genny.life:3310/gennydb?zeroDateTimeBehavior=convertToNull&serverTimezone=UTC
export MYSQL_USER=genny
export MYSQL_PASSWORD=password
export INFINISPAN_URL=alyson.genny.life:11222
export INFINISPAN_USERNAME=genny
export INFINISPAN_PASSWORD=password
export PROJECT_REALM=internmatch
export PROJECT_URL=https://internmatch.genny.life
export realm=mentormatch
./mvnw clean  quarkus:dev -Ddebug=5558 -DskipTests=true  -Dinfinispan.client.hotrod.server_list=${gennyhost}:11222 -Dinfinispan.client.hotrod.client_intelligence=BASIC  
#./mvnw clean  quarkus:dev -Ddebug=5558 -Dquarkus.http.port=${port} -DskipTests=true -Dinfinispan.client.hotrod.server_list=10.123.123.123:11222 -Dinfinispan.client.hotrod.client_intelligence=BASIC

