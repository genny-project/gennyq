#!/bin/bash
set -e

realm=`echo $GENNY_KEYCLOAK_REALM`
username=`echo $TEST_USER_NAME`
password=`echo $TEST_USER_PASSWORD`
#username=`echo $GENNY_SERVICE_USERNAME`
#password=`echo $GENNY_SERVICE_PASSWORD`
clientid=`echo $GENNY_CLIENT_ID`
secret=`echo $GENNY_CLIENT_SECRET`

#KEYCLOAK_RESPONSE=`curl -s -X POST ${GENNY_KEYCLOAK_URL:https://keycloak.gada.io}/realms/${GENNY_REALM:internmatch}/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username=testuser@gada.io' -d 'password='$password'' -d 'grant_type=password' -d 'client_id='$clientid''  -d 'client_secret='$secret''`
KEYCLOAK_RESPONSE=`curl -s -X POST ${GENNY_KEYCLOAK_URL}/realms/${GENNY_REALM:internmatch}/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username='$username'' -d 'password='$password'' -d 'grant_type=password' -d 'client_id='$clientid''  -d 'client_secret='$secret''`
#KEYCLOAK_RESPONSE=`curl -s -X POST ${GENNY_KEYCLOAK_URL}/realms/${GENNY_REALM:internmatch}/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username=testuser@gada.io' -d 'password='$password'' -d 'grant_type=password' -d 'client_id='$clientid''  -d 'client_secret='$secret''`

ACCESS_TOKEN=`echo "$KEYCLOAK_RESPONSE" | jq -r '.access_token'`
echo ${ACCESS_TOKEN}  

