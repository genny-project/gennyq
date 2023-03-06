#!/bin/bash
jps -l | grep bootq | cut -d" " -f1 | xargs kill -9

