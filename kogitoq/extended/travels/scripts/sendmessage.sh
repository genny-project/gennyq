#!/bin/bash
name=$1
#curl -d "messageCode=${name}" -H "Content-Type: application/x-www-form-urlencoded" -X POST http://localhost:9580/messages?businessKey=123
curl -H "Content-Type: application/json" -H "Accept: application/json" -X POST http://localhost:9580/messages -d @- << EOF
{   
        "messageCode": "${name}"
}
EOF
echo ""
