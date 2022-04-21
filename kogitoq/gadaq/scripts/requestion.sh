#!/bin/bash
processId=$1
port="${2:-9580}"
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "ProcessId passed is $processId"
echo ''
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson2.genny.life:${port}/processquestions/${processId}/requestion 

echo ""
