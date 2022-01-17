#!/bin/sh
#
# Copyright 2021 Red Hat, Inc. and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "Script requires your Kogito Travel Agency and Visas projects to be compiled"

PROJECT_VERSION=$(cd ../ && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

echo "Project version: ${PROJECT_VERSION}"

if [[ $PROJECT_VERSION == *SNAPSHOT ]];
then
  KOGITO_VERSION="latest"
else
  KOGITO_VERSION=${PROJECT_VERSION%.*}
fi

echo "Kogito Image version: ${KOGITO_VERSION}"
echo "KOGITO_VERSION=${KOGITO_VERSION}" > ".env"

if [ "$(uname)" == "Darwin" ]; then
   #echo "DOCKER_GATEWAY_HOST=${host.docker.internal}" >> ".env"
   echo "DOCKER_GATEWAY_HOST=10.123.123.123" >> ".env"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
   echo "DOCKER_GATEWAY_HOST=172.17.0.1" >> ".env"
fi

PERSISTENCE_FOLDER=./target/protobuf
KOGITO_TRAVEL_AGENCY_PERSISTENCE=../travels/target/classes/META-INF/resources/persistence/protobuf
KOGITO_VISAS_PERSISTENCE=../visas/target/classes/META-INF/resources/persistence/protobuf

mkdir -p $PERSISTENCE_FOLDER

if [ -d "$KOGITO_TRAVEL_AGENCY_PERSISTENCE" ]
then
    cp $KOGITO_TRAVEL_AGENCY_PERSISTENCE/*.proto $PERSISTENCE_FOLDER
else
    echo "$KOGITO_TRAVEL_AGENCY_PERSISTENCE does not exist. Have you compiled your Kogito Travel Agency project?"
    exit 1
fi

if [ -d "$KOGITO_VISAS_PERSISTENCE" ]
then
    cp $KOGITO_VISAS_PERSISTENCE/*.proto $PERSISTENCE_FOLDER
else
    echo "$KOGITO_VISAS_PERSISTENCE does not exist. Have you compiled your Kogito Visas project?"
    exit 1
fi

SVG_FOLDER=./svg

KOGITO_TRAVEL_SVG_FOLDER=../travels/target/classes/META-INF/processSVG
KOGITO_VISAS_SVG_FOLDER=../visas/target/classes/META-INF/processSVG

mkdir -p $SVG_FOLDER

if [ -d "$KOGITO_TRAVEL_SVG_FOLDER" ]
then
    cp $KOGITO_TRAVEL_SVG_FOLDER/*.svg $SVG_FOLDER
else
    echo "$KOGITO_TRAVEL_SVG_FOLDER does not exist. Have you compiled Kogito Travel Agency project?"
    exit 1
fi

if [ -d "$KOGITO_VISAS_SVG_FOLDER" ]
then
    cp $KOGITO_VISAS_SVG_FOLDER/*.svg $SVG_FOLDER
else
    echo "$KOGITO_VISAS_SVG_FOLDER does not exist. Have you compiled Kogito Visas project?"
    exit 1
fi
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280
export GENNY_KAFKA_URL=localhost:9092
export GENNY_CLIENT_ID=kogito-console-quarkus
export GENNY_REALM=internmatch
#export GENNY_KOGITO_SERVICE_URL=http://office.crowtech.com.au:8579
echo $GENNY_KEYCLOAK_URL
echo $GENNY_SERVICE_USERNAME
echo $GENNY_SERVICE_PASSWORD
echo $GENNY_CLIENT_ID
echo $GENNY_CLIENT_SECRET
echo $GENNY_REALM
export GENNY_INFINISPAN_URL=localhost:11222
echo $GENNY_INFINISPAN_URL
echo $GENNY_INFINISPAN_USERNAME
echo $GENNY_INFINISPAN_PASSWORD
echo $GENNY_KAFKA_URL
echo $GENNY_KOGITO_DATAINDEX_HTTP_URL
echo $GENNY_KOGITO_DATAINDEX_WS_URLA
echo $GENNY_KOGITO_JOBSERVICE_URL
echo $GENNY_KOGITO_SERVICE_URL
docker-compose up
