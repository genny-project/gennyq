#!/bin/bash
name=$1
curl -H "Content-Type: application/json" -H "Accept: application/json" -X POST http://localhost:8580/hiring -d @- << EOF
{   
    "candidate": {
        "name": "${name}",
        "email": "harrypotter@example.com",
        "salary": 30000,
        "skills": "Java, Kogito"
    }
}
EOF
echo ""
