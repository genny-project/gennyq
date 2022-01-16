#!/bin/bash
jps -l | grep kogitoq | cut -d" " -f1 | xargs kill -9

