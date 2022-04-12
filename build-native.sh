#!/bin/bash
./mvnw clean package -Pnative -DskipTests=true -Dcheckstyle.skip 
