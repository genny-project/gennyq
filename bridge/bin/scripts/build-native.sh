#!/bin/bash

source ../.env

cd ../

./mvnw package -Pnative

cd -

