#!/bin/bash
processId="${1:-4e580190-43f7-4842-9b0f-4080734f5d6f}"
echo "processId=${processId}"
attributeCode="${2:-PRI_FIRSTNAME}"
value="${3:-Shazzy}"
echo "Value=${value}"
#TOKEN=`./gettoken-prod.sh`
TOKEN=`./gettoken-cache.sh`
echo ''
#echo $TOKEN
echo ''
questionCode=QUE_LASTNAME
sourceCode=PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943
targetCode=PER_34EB0455-1DC0-4121-80ED-90C0B9EEA413

payload="{\"items\":[{\"askId\":349572,\"processId\":\"${processId}\",\"attributeCode\":\"${attributeCode}\",\"sourceCode\":\"PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943\",\"targetCode\":\"${targetCode}\",\"code\":\"${questionCode}\",\"identifier\":\"${questionCode}\",\"weight\":1,\"value\":\"${value}\",\"inferred\":false}],\"token\":\"${TOKEN}\",\"msg_type\":\"DATA_MSG\",\"event_type\":false,\"redirect\":false,\"data_type\":\"Answer\"}"
#echo $payload
echo  $payload > answer.json


#docker exec -i $(docker ps -a | grep "kafka" | grep Up | awk '{print $1}') bash -c " \
docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic valid_data" < answer.json

echo "Answer sent"
