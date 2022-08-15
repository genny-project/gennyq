#!/bin/bash
mvn clean
./mvnw clean install -DskipTests=true -Dcheckstyle.skip
#./mvnw clean install -DskipTests=false -Dcheckstyle.skip
