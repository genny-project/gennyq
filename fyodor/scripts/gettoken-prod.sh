#!/bin/bash
set -e
password=`echo $USER_PASSWORD`
secret=`echo $GENNY_CLIENT_SECRET`
clientid=`echo $GENNY_CLIENT_ID`
#clientid=internmatch
#echo "password = $password"
#echo "clientid = $clientid"
#echo "secret = $secret"
KEYCLOAK_RESPONSE=`curl -s -X POST https://keycloak-bali.gada.io/realms/internmatch/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username=testuser@gada.io' -d 'password='$password'' -d 'grant_type=password' -d 'client_id='$clientid''  -d 'client_secret='$secret''`
#KEYCLOAK_RESPONSE=`curl -s -X POST https://keycloak-bali.gada.io/realms/internmatch/protocol/openid-connect/token  -H "Content-Type: application/x-www-form-urlencoded" -d 'username=testuser@gada.io' -d 'password='$password'' -d 'grant_type=password' -d 'client_id='$clientid' '`
#echo $KEYCLOAK_RESPONSE
#printf "${RED}Parsing access_token field, as we don't need the other elements:${NORMAL}\n"
ACCESS_TOKEN=`echo "$KEYCLOAK_RESPONSE" | jq -r '.access_token'`
echo ${ACCESS_TOKEN}  

