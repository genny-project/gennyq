#!/bin/bash
jps -l | grep gadaq | cut -d" " -f1 | xargs kill -9

