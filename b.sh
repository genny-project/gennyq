#!/bin/bash
cd qwandaq;./build.sh;cd ../serviceq;./build.sh;cd kogitoq/kogito-common;./build.sh;cd ../gadaq;./build.sh;./build-docker.sh;cd ../bridge;./build.sh;./build-docker.sh;cd ../lauchy;./build.sh;./build-docker.sh;cd ../dropkick;./build.sh;./build-docker.sh;cd ../fyodor;./build.sh;./build-docker.sh;cd ..
