#!/bin/bash
set -e
if [ "$#" -eq 0 ]; then
	echo "Usage: $0 <product code>"
	exit 1
fi

realm=internmatch
key=TOKEN:PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752
TOKEN=`./gettoken-prod.sh $1`
CACHEREAD=`curl -X GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Bearer $TOKEN"  "http://alyson7.genny.life:4242/cache/${key}"`
echo $CACHEREAD
