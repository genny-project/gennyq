#!/bin/bash
project=`echo "${PWD##*/}" | tr '[:upper:]' '[:lower:]'`
project=shleemy
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
#version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version=$(cat src/main/resources/${project}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
else
  version="${1}"
fi

echo "project = ${project}"
echo "org= ${org}"
echo "version = ${version}"
USER=`whoami`
export JAVA_OPTS_APPEND=-Xmx512M
JAVA_OPTS="-Xms256m -Xmx1024m -Djava.net.preferIPv4Stack=true -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError" ./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true -Dstyle.color=always
docker tag ${org}/${project}:${version} ${org}/${project}:${version}
docker tag ${org}/${project}:${version} ${org}/${project}:latest
docker tag ${org}/${project}:${version} ${org}/${project}:ptest
