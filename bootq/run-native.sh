#!/bin/bash
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=${GENNY_KEYCLOAK_URL:https://keycloak-testing.gada.io/auth}
export GENNY_KEYCLOAK_REALM=${GENNY_KEYCLOAK_REALM:internmatch}
export GENNY_API_URL=http://internmatch.genny.life:8280
echo "keycloak url="$GENNY_KEYCLOAK_URL
echo "keycloak realm="$GENNY_KEYCLOAK_REALM
echo "service username="$GENNY_SERVICE_USERNAME
echo "service pass="$GENNY_SERVICE_PASSWORD
echo "client id="$GENNY_CLIENT_ID
echo "client secret="$GENNY_CLIENT_SECRET

./target/bootq-9.10.0-runner
