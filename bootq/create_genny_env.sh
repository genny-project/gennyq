#!/bin/bash

ENV_FILE=genny.env
ENV_FILE_APP=${ENV_FILE}.json


if [ -z "${2}" ];then
echo "No ip supplied, determining local host ip ...."
myip=
while IFS=$': \t' read -a line ;do
    [ -z "${line%inet}" ] && ip=${line[${#line[1]}>4?1:2]} &&
        [ "${ip#127.0.0.1}" ] && myip=$ip
  done< <(LANG=C /sbin/ifconfig)


if [ -z "${myip}" ]; then
   myip=127.0.0.1
fi

else
echo "ip supplied... $2"
myip=$2

fi
echo $myip
CLUSTER_IP=127.0.0.1

#myip=127.0.0.2
docker volume create cassandra_data
docker volume create mysql_data

#create env file
MYSQL_PORT=3306
MYSQL_DB=gennydb
MYSQL_URL=mysql
MYSQL_PASSWORD=password
FULL_MYSQL_URL="jdbc:mysql://${MYSQL_URL}:${MYSQL_PORT}/${MYSQL_DB}?allowPublicKeyRetrieval=true&syscharacterEncoding=utf8mb4&useSSL=FALSE&serverTimezone=UTC&rewriteBatchedStatements=true"
#FULL_MYSQL_URL=jdbc:mysql://${MYSQL_URL}:${MYSQL_PORT}/${MYSQL_DB}?allowPublicKeyRetrieval=true&syscharacterEncoding=utf8mb4&useSSL=FALSE&serverTimezone=UTC&rewriteBatchedStatements=true
MYSQL_USER=genny

#MYSQL_PASSWORD=password
MYSQL_ROOT_PASSWORD=password

MYSQL_ROOT_HOST=${HOSTIP}
echo "" >> $ENV_FILE
echo "KEYCLOAKPORT=${KEYCLOAK_PORT}" >> $ENV_FILE
echo "KEYCLOAKPROTO=${KEYCLOAK_PROTO}" >> $ENV_FILE
echo "KEYCLOAKURL=https://bouncer.outcome-hub.com" >> $ENV_FILE
#echo "KEYCLOAKURL=${KEYCLOAK_PROTO}${myip}:${KEYCLOAK_PORT}" >> $ENV_FILE
echo "KEYCLOAK_USERNAME=${KEYCLOAK_USERNAME}" >> $ENV_FILE
echo "KEYCLOAK_SERVICE_ID=${KEYCLOAK_SERVICE_ID}" >> $ENV_FILE
echo "KEYCLOAK_PASSWORD=${KEYCLOAK_PASSWORD}" >> $ENV_FILE
echo "KEYCLOAK_SECRET=${KEYCLOAK_SECRET}" >> $ENV_FILE
echo "CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}" >> $ENV_FILE
echo "" >> $ENV_FILE
echo "MYSQL_URL=${MYSQL_URL}" >> $ENV_FILE
echo "MYSQL_DB=${MYSQL_DB}" >> $ENV_FILE
echo "MYSQL_DATABASE=${MYSQL_DB}" >> $ENV_FILE
echo "MYSQL_PORT=${MYSQL_PORT}" >> $ENV_FILE
echo "MYSQL_USER=${MYSQL_USER}" >> $ENV_FILE
echo "MYSQL_PASSWORD=${MYSQL_PASSWORD}" >> $ENV_FILE
echo "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" >> $ENV_FILE
echo "MYSQL_ALLOW_EMPTY=${MYSQL_ALLOW_EMPTY}" >> $ENV_FILE

GOOGLE_SVC_ACC_PATH=/root/.genny/sheets.googleapis.com-java-quickstart/token-secret-service-account.json
echo "GOOGLE_SVC_ACC_PATH=${GOOGLE_SVC_ACC_PATH}" >> $ENV_FILE

echo ""
echo "###### Run Settings ######"
cat $ENV_FILE

#find all the projects
for i in ` ind .. -mindepth 1 -maxdepth 1 -type d | grep prj  | awk -F "/" '{ print $2 }'`do
   echo $i
end
