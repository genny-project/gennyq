#!/bin/bash
productCode=${1}
processId=${2}
targetCode=${3}
echo "processId=${processId}"
./sendanswer.sh ${productCode} ${processId} ${targetCode} PRI_FIRSTNAME John 
./sendanswer.sh ${productCode} ${processId} ${targetCode} PRI_LASTNAME Doe 
./sendanswer.sh ${productCode} ${processId} ${targetCode} PRI_EMAIL john.doe@gmail.com
./sendanswer.sh ${productCode} ${processId} ${targetCode} PRI_MOBILE 61434321230 
echo "All Answers sent"
