#!/bin/bash
#kafkacat -b 192.168.86.37:9092 -C -o beginning -q -t  events 
kafkacat -b alyson.genny.life:9092 -C -o beginning -q -t  kogito-usertaskinstances-events
#kafkacat -b alyson.genny.life:9092 -C -o beginning -q -t  kogito-processinstances-events


