#!/bin/bash
project=${PWD##*/}
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
version=$(prop 'git.build.version')

./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true 
docker tag ${org}/${project}:${version} ${org}/${project}:latest
