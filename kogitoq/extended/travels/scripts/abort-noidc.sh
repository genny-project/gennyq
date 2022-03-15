#!/bin/bash
abortCode="${1:-ABORT_SIGNAL}"
port="${2:-8580}"
bookingNumber=123
id=2d7648dd-d570-45ee-98b2-059aff2570c7
TOKEN=`./gettoken-prod.sh`
echo ''
echo $TOKEN
echo "MessageCode3 passed is $messageCode - testing BaseEntity"
echo ''
curl   -H "Content-Type: application/json"  -H "Accept: application/json" -X POST http://alyson.genny.life:${port}/travels/${id}/ABORT_SIGNAL/ -d @- << EOF
{
  "id": "string",
  "flight": {
    "flightNumber": "string",
    "seat": "string",
    "gate": "string",
    "departure": "2022-03-15",
    "arrival": "2022-03-15"
  },
  "trip": {
    "city": "string",
    "country": "string",
    "begin": "2022-03-15",
    "end": "2022-03-15",
    "visaRequired": true
  },
  "hotel": {
    "name": "string",
    "address": {
      "street": "string",
      "city": "string",
      "zipCode": "string",
      "country": "string"
    },
    "phone": "string",
    "bookingNumber": "string",
    "room": "string"
  },
  "visaApplication": {
    "firstName": "string",
    "lastName": "string",
    "city": "string",
    "country": "string",
    "duration": 0,
    "passportNumber": "string",
    "nationality": "string",
    "approved": true
  },
  "traveller": {
    "firstName": "string",
    "lastName": "string",
    "email": "string",
    "nationality": "string",
    "address": {
      "street": "string",
      "city": "string",
      "zipCode": "string",
      "country": "string"
    }
  }
}
EOF
echo ""
