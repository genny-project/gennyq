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
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson2.genny.life:${port}/travels/${id}/ABORT_SIGNAL 

echo ""
