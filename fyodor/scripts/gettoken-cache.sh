#!/bin/bash
realm=internmatch
key=TOKEN:PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752
TOKEN=`./gettoken-prod.sh`

CACHEREAD=`curl -s -X GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Bearer $TOKEN"  "http://alyson7.genny.life:8280/service/cache/read/${realm}/${key}"`
CR=`echo "$CACHEREAD" | jq -r '.value'`
echo $CR
