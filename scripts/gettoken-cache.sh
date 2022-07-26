#!/bin/bash
set -e
if [ "$#" -eq 0 ]; then
	echo "Usage: $0 <productCode>"
	exit 1
fi
PRODUCT_CODE=$1
KEY="TOKEN:PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752"
TOKEN=`./gettoken-prod.sh`
CACHEREAD=`curl -s GET --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Bearer $TOKEN"  "http://alyson7.genny.life:10070/cache/${PRODUCT_CODE}/${KEY}"`
echo $CACHEREAD
