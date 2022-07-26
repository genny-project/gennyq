#!/bin/bash
productCode=${1:-internmatch}
questionCode=${2:-QUE_BASEENTITY_GRP}
targetCode=${3:-PER_F6BE8CA8-14CA-4BA7-9327-F435C7BE4042}
sourceCode=PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752
TOKEN=`./gettoken-cache.sh ${productCode}`
#echo $TOKEN
payload="{\"data\":{\"sourceCode\":\"${sourceCode}\",\"targetCode\":\"${targetCode}\",\"code\":\"TEST_${questionCode}\"},\"token\":\"${TOKEN}\",\"msg_type\":\"EVT_MSG\"}"
echo $payload
echo  $payload > event.json

docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic events" < event.json

echo "Submit sent"
