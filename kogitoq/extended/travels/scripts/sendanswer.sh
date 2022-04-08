#!/bin/bash
port="${1:-9580}"
#TOKEN=`./gettoken-prod.sh`
TOKEN=`./gettoken-cache.sh`
echo ''
echo $TOKEN
echo ''
processId=920130ba-43a7-439a-b822-ffcd1a6f9dab
attributeCode=PRI_LASTNAME
questionCode=QUE_LASTNAME
sourceCode=PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943
targetCode=PER_34EB0455-1DC0-4121-80ED-90C0B9EEA413

echo  "{\"items\":[{\"askId\":349572,\"processId\":\"${processId}\",\"attributeCode\":\"PRI_FIRSTNAME\",\"sourceCode\":\"PER_086CDF1F-A98F-4E73-9825-0A4CFE2BB943\",\"targetCode\":\"${targetCode}\",\"code\":\"QUE_FIRSTNAME\",\"identifier\":\"QUE_FIRSTNAME\",\"weight\":1,\"value\":\"Varun\",\"inferred\":false}],\"token\":\"${TOKEN}\",\"msg_type\":\"DATA_MSG\",\"event_type\":false,\"redirect\":false,\"data_type\":\"Answer\"}" > answer.json


#docker exec -i $(docker ps -a | grep "kafka" | grep Up | awk '{print $1}') bash -c " \
docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic valid_data" < answer.json

echo ""
