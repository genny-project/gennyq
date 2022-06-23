#!/bin/bash
TOKEN=${1}
processId=${2}
questionCode=QUE_SUBMIT

payload="{\"data\":{\"code\":\"${questionCode}\",\"processId\":\"${processId}\"},\"token\":\"${TOKEN}\",\"msg_type\":\"EVT_MSG\"}"
#echo $payload
echo  $payload > answer.json

docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic events" < answer.json

echo "Submit sent"

