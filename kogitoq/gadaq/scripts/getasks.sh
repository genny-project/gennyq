#!/bin/bash
messageCode="${1:-MSG_IM_INTERN_LOGBOOK_REMINDER}"
port="${2:-9580}"
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "MessageCode passed is $messageCode"
echo ''
#curl -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
#curl  --header "Authorization: Bearer $TOKEN"  -d "messageCode={\"code\":\"${messageCode}\",\"bookingNumber\":\"${bookingNumber}\"}" -H "Content-Type: application/json" -X POST http://localhost:9580/messages?businessKey=123
#curl  -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:9580/messages?businessKey=${bookingNumber} -d @- << EOF
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X GET http://alyson2.genny.life:${port}/workitemhelper/questions/internmatch/QUE_PROJECT_SIDEBAR_GRP/PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943/PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943

echo ""
