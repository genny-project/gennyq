#!/bin/bash
if [ "$#" -lt 3 ]; then
	echo "Usage: $0 <productCode> <questionCode> <targetCode> (optional <port>)"
	exit 1
fi
productCode=${1}
questionCode=${2}
targetCode=${3}
port="${4:-6590}"
TOKEN=`./gettoken-cache.sh ${productCode}`
echo $TOKEN
payload="\
{\
	\"type\":\"start_process_questions\",\
	\"data\": {\
		\"code\": \"${questionCode}\",\
		\"sourceCode\": \"PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752\",\
		\"targetCode\": \"${targetCode}\",\
		\"parentCode\": \"PCM_FORM\",\
		\"token\":\"${TOKEN}\"\
	}\
}"

echo $payload > msg.json

docker exec -i kafka bash -c " \
printf '%s\n%s\n' 'security.protocol=PLAINTEXT' 'sasl.mechanism=PLAIN' > prop && \
kafka-console-producer --producer.config=prop  --bootstrap-server localhost:9092 --topic start_process_questions" < msg.json
