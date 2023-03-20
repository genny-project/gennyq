#!/bin/zsh
DOMAIN=localhost:8098
TABLE=$1
SHEET_ID=${2:-${GENNY_MULTITENANCY_SHEET_ID}}

if [ -z "$1" ]; then
  echo "No argument supplied, loading all tables"
  batchloadingResult=`curl -X GET  --header 'Accept: text/plain' "${DOMAIN}/bootq/loadsheets/${SHEET_ID}"`
else
  echo "Loading Table: "${TABLE}
  batchloadingResult=`curl -X GET  --header 'Accept: text/plain' "${DOMAIN}/bootq/loadsheets/${SHEET_ID}/${TABLE}"`
fi

echo "Result: "$batchloadingResult
