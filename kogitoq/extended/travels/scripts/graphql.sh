#!/bin/bash
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo ''
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:8582 -d @- << EOF

{
  Travels:(where: {Application: {agentCode: {like: "PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943*"}}}) {
    Application {
      internCode 
      agentCode 
    }
  }
}


EOF

echo ""
