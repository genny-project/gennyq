#!/bin/bash
productCode=${1}
questionCode=${2}
targetCode=${3}
port="${4:-6590}"
TOKEN=`./gettoken-cache.sh ${productCode}`

curl -X POST "http://alyson2.genny.life:${port}/processQuestions"  -H "Content-Type: application/json"  -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -d @- << EOF
{
	"questionCode" : "${questionCode}",
	"sourceCode"   : "PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752",
	"targetCode"   : "${targetCode}",
	"pcmCode"      : "PCM_FORM"
}
EOF
