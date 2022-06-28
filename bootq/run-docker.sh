#!/bin/bash
 ./create_genny_env.sh genny.env 10.123.123.123 >& /dev/null
export ENV_FILE=genny.env
docker-compose stop
docker-compose rm -f
docker-compose up -d
#docker-compose logs -f bootq

