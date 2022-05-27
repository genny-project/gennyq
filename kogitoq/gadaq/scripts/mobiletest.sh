#!/bin/bash
pid=`./process-questions.sh | tail -n 1 | jq -r '.id'`
echo $pid
./sendanswers.sh ${pid}


