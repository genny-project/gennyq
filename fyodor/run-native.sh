#!/bin/bash
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak-bali.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280
echo $GENNY_KEYCLOAK_URL
echo $GENNY_SERVICE_USERNAME
echo $GENNY_SERVICE_PASSWORD
echo $GENNY_CLIENT_ID
echo $GENNY_CLIENT_SECRET
echo $GENNY_REALM
export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak-bali.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280
export GENNY_SHOW_VALUES=TRUE; 

./target/fyodor-9.10.0-runner
