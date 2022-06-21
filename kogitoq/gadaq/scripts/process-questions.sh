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
	"sourceCode"   : "PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943",
	"targetCode"   : "APP_FFFEE841-0FD3-4C38-83E6-F3C26784C32C",
	"pcmCode"      : "PCM_INTERN",
	"userTokenStr" : "${TOKEN}"
}
EOF

echo ""
