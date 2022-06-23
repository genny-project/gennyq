#!/bin/bash
messageTemplateCode=$1
recipientBeCode=$2
port="${3:-9580}"
realm="${4:-alyson}"
echo  "Getting token for realm.... ${realm}"
TOKEN=`./gettoken-cache.sh ${realm}`
echo ''
echo $TOKEN
echo ''
curl -v -H "Content-Type: application/json"  -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST http://alyson2.genny.life:${port}/endMessageTest -d @- << EOF
{
        "pcmCode" : "${PCM}",
        "loc"     : "${LOC}",
        "newValue": "${NEW}"
}
EOF
echo ""
