#!/bin/bash
jps -l | grep travels | cut -d" " -f1 | xargs kill -9

