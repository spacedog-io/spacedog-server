#!/bin/bash

### an alternative is to use install=`dirname $0`
### to get the home directory of this script
### but this does not work with scripts launched
### from symbolic link

if [ "X$DOG_HOME" == "X" ]
then
	echo "Variable DOG_HOME must be set with the absolute path (do not use ~) to the dog CLI install directory."
	exit -1
fi

java -d64 -cp "$DOG_HOME/lib/*" io.spacedog.cli.DogCLI "$@"