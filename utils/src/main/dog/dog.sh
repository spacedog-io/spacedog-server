#!/bin/bash
install=`dirname $0`
java -d64 -cp "$install/lib/*" io.spacedog.client.DogCLI "$@"