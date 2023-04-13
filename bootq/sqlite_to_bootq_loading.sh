#!/bin/zsh
DOMAIN=localhost:8098
REALM=$1
DB_NAMES=$2

if [ $# != 2 ]; then
  echo "Wrong number of arguments supplied. Usage: ${0} internmatch Genny,Internmatch1,Internmatch2"
  exit 1
else
  echo "Loading..." 
  batchloadingResult=`curl -X GET  --header 'Accept: text/plain' "${DOMAIN}/bootq/loadsqlitetodb/${REALM}/${DB_NAMES}"`
fi

echo "Result: "$batchloadingResult
