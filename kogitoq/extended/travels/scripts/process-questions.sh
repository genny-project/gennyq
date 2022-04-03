#!/bin/bash
questionCode="${1:-QUE_INTERN_GRP}"
port="${2:-9580}"
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "QuestionCode passed is $questionCode"
echo ''
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson2.genny.life:${port}/processquestions -d @- << EOF
{
	"questionCode" : {
        	"code" : "${questionCode}"
	}
}
EOF

echo ""
