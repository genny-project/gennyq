#!/bin/bash
abortCode="${1:-ABORT_SIGNAL}"
port="${2:-9580}"
bookingNumber=123
id=123
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "MessageCode3 passed is $messageCode - testing BaseEntity"
echo ''
#curl -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
#curl  --header "Authorization: Bearer $TOKEN"  -d "messageCode={\"code\":\"${messageCode}\",\"bookingNumber\":\"${bookingNumber}\"}" -H "Content-Type: application/json" -X POST http://localhost:9580/messages?businessKey=123
#curl  -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:9580/messages?businessKey=${bookingNumber} -d @- << EOF
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson2.genny.life:${port}/travels/{id}/ABORT_SIGNAL -d @- << EOF
{
     "abortCode" : {
     }
}
EOF

echo ""
