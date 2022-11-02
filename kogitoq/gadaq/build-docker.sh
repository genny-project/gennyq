#!/bin/bash
cp -rp ../../docker/* src/main/docker/
PROJECT_VERSION=$(cd ../ && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
project=`echo "${PWD##*/}" | tr '[:upper:]' '[:lower:]'`
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
#version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
  version=$PROJECT_VERSION
else
  version="${1}"
fi

echo "project = ${project}"
echo "org= ${org}"
echo "version = ${version}"
USER=`whoami`
QUARKUS_DOCKER_BUILDX_PLATFORM=linux/amd64
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true  -Dquarkus.docker.buildx.platform=linux/amd64

docker tag ${org}/${project}:${version} ${org}/${project}:latest
docker tag ${org}/${project}:${version} ${org}/${project}:ptest
