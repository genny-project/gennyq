#!/bin/bash
messageCode="${1:-MSG_IM_INTERN_LOGBOOK_REMINDER}"
port="${2:-9580}"
bookingNumber=123
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "MessageCode2 passed is $messageCode - testing BaseEntity"
echo ''
#curl -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
#curl  --header "Authorization: Bearer $TOKEN"  -d "messageCode={\"code\":\"${messageCode}\",\"bookingNumber\":\"${bookingNumber}\"}" -H "Content-Type: application/json" -X POST http://localhost:9580/messages?businessKey=123
#curl  -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:9580/messages?businessKey=${bookingNumber} -d @- << EOF
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:${port}/messages2?businessKey=${bookingNumber} -d @- << EOF
{
     "messageCode" : {
		"id":39996,"name":"Bob Console","realm":"internmatch","code":"${messageCode}","index":0,"status":"ACTIVE","baseEntityAttributes":[],"fromCache":false,"questions":[]
     }
}
EOF

echo ""
