#!/bin/bash
messageCode=$1

TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo ''
#curl -d "messageCode=${name}" -H "Content-Type: application/x-www-form-urlencoded" -X POST http://localhost:9580/messages?businessKey=123
#curl -H "Content-Type: application/json" --header "Authorization: Bearer $TOKEN"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
curl -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
{   
        "messageCode": "${messageCode}"
}
EOF
echo ""
