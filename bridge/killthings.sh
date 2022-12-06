#!/bin/bash
jps -l | grep bridge | cut -d" " -f1 | xargs kill -9

