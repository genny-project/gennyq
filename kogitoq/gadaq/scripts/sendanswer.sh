#!/bin/bash
productCode=${1}
processId=${2}
targetCode=${3}
attributeCode=${4}
value=${5}

TOKEN=`./gettoken-cache.sh ${productCode}`

payload="{\"items\":[{\"askId\":349572,\"processId\":\"${processId}\",\"attributeCode\":\"${attributeCode}\",\"sourceCode\":\"PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943\",\"targetCode\":\"${targetCode}\",\"weight\":1,\"value\":\"${value}\",\"inferred\":false}],\"token\":\"${TOKEN}\",\"msg_type\":\"DATA_MSG\",\"event_type\":false,\"redirect\":false,\"data_type\":\"Answer\"}"
echo  $payload > answer.json

docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic valid_data" < answer.json

echo "Answer sent"
