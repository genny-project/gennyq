#!/bin/bash
questionCode="${1:-QUE_INTERN_GRP}"
port="${2:-9580}"
#TOKEN=`./gettoken-prod.sh`
TOKEN=`./gettoken-cache.sh alyson`
echo ''
echo $TOKEN
echo "QuestionCode passed is $questionCode"
echo ''
curl -s   -H "Content-Type: application/json"  -H "Accept: application/json" -H "Authorization: Bearer $TOKEN" -X POST http://alyson2.genny.life:${port}/processquestions -d @- << EOF
{
	"questionCode" : "${questionCode}",
	"sourceCode"   : "PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752",
	"targetCode"   : "PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752",
	"pcmCode"      : "PCM_INTERN",
	"userTokenStr" : "${TOKEN}"
}
EOF

echo ""
