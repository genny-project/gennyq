#!/bin/bash

#./mvnw clean install -DskipTests=true -Dcheckstyle.skip
#./mvnw clean install -DskipTests=false -Dcheckstyle.skip

#./build-dependency.sh life.genny:kogito-common:10.1.0
#./mvnw clean install -DskipTests=true -Dcheckstyle.skip dependency:get -Dartifact=life.genny:kogito-common:10.1.0

artifact=$1
if [[ -z $artifact ]];then
  ./mvnw clean install -DskipTests=true -Dcheckstyle.skip
else
  ./mvnw clean install -DskipTests=true -Dcheckstyle.skip dependency:get -Dartifact=$artifact
fi

