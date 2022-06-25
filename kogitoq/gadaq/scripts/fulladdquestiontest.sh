#!/bin/bash
productCode="lojing"
questionCode="QUE_TENANT"
targetCode="PER_0F6169E1-FDD5-4DAF-BEC3-4126C6626752"

echo "Running"

pid=`./process-questions.sh ${productCode} ${questionCode} ${targetCode} | tail -n 1 | jq -r '.id'`
echo $pid
./sendanswers.sh ${productCode} ${pid} ${targetCode}
./sendsubmit.sh ${productCode} ${pid} ${targetCode}

