#!/bin/bash
PROTO_PATH=src/main/resources/META-INF/protobuf
rm -rf $PROTO_PATH/*
cp ./kogitoq/gadaq/target/classes/META-INF/resources/persistence/protobuf/* $PROTO_PATH/
