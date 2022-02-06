#!/bin/bash
messageCode="${1:-MSG_IM_INTERN_LOGBOOK_REMINDER}"

TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo ''
#curl -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
#curl -d "messageCode=${name}" -H "Content-Type: application/x-www-form-urlencoded" -X POST http://localhost:9580/messages?businessKey=123
curl -H "Content-Type: application/json" --header "Authorization: Bearer $TOKEN"  -H "Accept: application/json" -X POST http://alyson.genny.life:9580/messages -d @- << EOF
{   
        "messageCode": "${messageCode}"
}
EOF
echo ""
