#!/bin/bash
PCM=$1
LOC=$2
NEW=$3
port="${4:-9580}"
realm="${5:-alyson}"
echo  "Getting token for realm.... ${realm}"
TOKEN=`./gettoken-cache.sh ${realm}`
echo ''
echo $TOKEN
echo ''
curl -v -H "Content-Type: application/json"  -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST http://alyson2.genny.life:${port}/updatepcm -d @- << EOF
{
	"pcmCode" : "${PCM}",
	"loc"     : "${LOC}",
	"newValue": "${NEW}"
}
EOF
echo ""
