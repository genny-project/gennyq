#!/bin/zsh
DOMAIN=localhost:8098
SHEET_ID=${1:-${GENNY_MULTITENANCY_SHEET_ID}}
TABLE=$2

if [ -z "$1" ]; then
  echo "No argument supplied, loading all tables"
  batchloadingResult=`curl -X GET  --header 'Accept: text/plain' "${DOMAIN}/bootq/loadsheetstosqlite/${SHEET_ID}"`
else
  echo "Loading Table: "${TABLE}
  batchloadingResult=`curl -X GET  --header 'Accept: text/plain' "${DOMAIN}/bootq/loadsheetstosqlite/${SHEET_ID}/${TABLE}"`
fi

echo "Result: "$batchloadingResult
