#!/bin/zsh
if [ "$#" -ge 1 ]; then
        ./build.sh $@
else
  ./build.sh qwandaq serviceq kogito-common
fi
