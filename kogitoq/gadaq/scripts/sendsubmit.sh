#!/bin/bash
productCode=${1}
processId=${2}
targetCode=${3}

TOKEN=`./gettoken-cache.sh ${productCode}`

payload="{\"data\":{\"code\":\"QUE_SUBMIT\",\"processId\":\"${processId}\",\"targetCode\":\"${targetCode}\"},\"token\":\"${TOKEN}\",\"msg_type\":\"EVT_MSG\"}"

echo  $payload > answer.json

docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic events" < answer.json

echo "Submit sent"

