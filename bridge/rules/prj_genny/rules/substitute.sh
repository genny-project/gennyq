#!/bin/bash

git grep -l '"EMAIL"' | xargs sed -i '' -e 's/"EMAIL"/QBaseMSGMessageType.EMAIL/g'
git grep -l '"SMS"' | xargs sed -i '' -e 's/"SMS"/QBaseMSGMessageType.SMS/g'
git grep -l '"TOAST"' | xargs sed -i '' -e 's/"TOAST"/QBaseMSGMessageType.TOAST/g'

