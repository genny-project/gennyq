#!/bin/bash
processId="${1:-4e580190-43f7-4842-9b0f-4080734f5d6f}"
echo "processId=${processId}"
./sendanswer.sh ${processId} PRI_FIRSTNAME Scott 
./sendanswer.sh ${processId} PRI_LASTNAME Morrison 
./sendanswer.sh ${processId} PRI_EMAIL shaz@bot.com
./sendanswer.sh ${processId} PRI_STUDENT_ID 8876876
./sendanswer.sh ${processId} PRI_MOBILE 61434321230 
./sendanswer.sh ${processId} PRI_TIME_ZONE Australia/Melbourne
./sendanswer.sh ${processId} LNK_DAYS_PER_WEEK 3
./sendanswer.sh ${processId} PRI_ADDRESS_FULL '17 Hardware Lane, MELBOURNE, VIC, 3000, AUSTRALIA'
./sendanswer.sh ${processId} LNK_OCCUPATION '[\"SEL_ENGINEER\"]'
./sendanswer.sh ${processId} LNK_INDUSTRY '[\"SEL_ENGINEERING\"]'
./sendanswer.sh ${processId} LNK_INTERNSHIP_DURATION '[\"SEL_12_WEEKS\"]'A
./sendanswer.sh ${processId} LNK_EDU_PROVIDER '[\"CPY_UNI_OF_MELB\"]'
./sendanswer.sh ${processId} PRI_START_DATE 2022-04-20'T'10:00:01
echo "Answers sent"
