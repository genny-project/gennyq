#!/bin/bash
realm=internmatch
key=TOKEN:PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943
TOKEN=`./gettoken-prod.sh`

CACHEREAD=`curl -s -X GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Bearer $TOKEN"  "http://alyson7.genny.life:4242/cache/${key}"`
CR=`echo "$CACHEREAD" | jq -r '.value'`
echo $CR
