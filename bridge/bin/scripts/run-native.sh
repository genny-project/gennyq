#!/bin/bash

image=$(for f in `ls ../target`; do  if [ -f ../target/$f ];then echo $f | grep runner; fi ; done)

source ../.env

cd ../


./target/$image

cd -
