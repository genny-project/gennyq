#!/bin/bash
PROTO_PATH=src/main/resources/META-INF/protobuf
rm -rf $PROTO_PATH/*
cp $KOGITO_GADAQ_AGENCY_PERSISTENCE/* $PROTO_PATH/
