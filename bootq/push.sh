#!/bin/bash
project=${PWD##*/}
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
version=$(prop 'git.build.version')

if [ -z "${1}" ]; then
  version="latest"
else
  version="${1}"
fi

if [ -f "$file" ]; then
  echo "$file found."
  echo "git.commit.id = " "$(prop 'git.commit.id')"
  echo "git.build.version = " "$(prop 'git.build.version')"

  docker push ${org}/${project}:"${version}"

  docker tag ${org}/${project}:"${version}" ${org}/${project}:latest
  docker push ${org}/${project}:latest

  docker tag ${org}/${project}:"${version}" ${org}/${project}:"$(prop 'git.build.version')"
  docker push ${org}/${project}:"$(prop 'git.build.version')"
else
  echo "ERROR: git properties $file not found."
fi
