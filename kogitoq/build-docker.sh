#!/bin/bash
mvn clean package -Dquarkus.container-image.build=true -DskipTests=true

