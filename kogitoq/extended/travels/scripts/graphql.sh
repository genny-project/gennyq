#!/bin/bash
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo ''
curl   -H "Content-Type: application/GraphQL"  -H "Accept: application/json" -X POST http://alyson.genny.life:8582/graphql -d @- << EOF
query {
  Application(where: {internCode: { like: "PER_A%" }}) {
    id
    internCode
    agentCode
  } 
}
EOF
echo ""
