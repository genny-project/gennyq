./#!/bin/bash
mvn clean
cd qwandaq;
mvn clean install -U
cd ../serviceq;
mvn clean install -U
cd ../kogitoq/kogito-common
mvn clean install -U
cd ../gadaq
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../../bridge
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../lauchy
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../dropkick
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../fyodor
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../messages
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ../shleemy
mvn clean package -U -DskipTests=true
./build-docker.sh
cd ..
