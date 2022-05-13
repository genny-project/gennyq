#!/bin/bash
mvn quarkus:remote-dev -Ddebug=false \
  -Dquarkus.package.type=mutable-jar \
  -Dquarkus.live-reload.url=http://localhost:8080 \
  -Dquarkus.live-reload.password=123

