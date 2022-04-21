#!/bin/bash
declare regex="QBaseMSGMessageType\;"
for i in `grep ./ -re "sendMessage" -l`; 
do
  if [ "$i" != ".//fixrules.sh" ]; then
    echo $i;
    declare file_content=$( cat "${i}" )
    if [[ " $file_content " =~ $regex ]];  then
#    if grep -Fxq "QBaseMSGMessageType;" $i; then
      echo "Already done"
    else
      perl -lne 'print $_;print "import life.genny.qwanda.message.QBaseMSGMessageType;" if(/\.QRules/);' $i > ./tmp ;cat ./tmp > $i ;
      echo "fixing"
    fi
  fi
done
