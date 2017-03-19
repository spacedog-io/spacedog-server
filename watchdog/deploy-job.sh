#!/bin/bash
#
# Use deploy-job <function-name> <function-version>
# to upload a new code package and update the function latest.
#

if [ -z "$1" ]
  then
    echo "Error: no function name supplied"
    echo "Usage: deploy-job <function-name> <code-version>"
    echo "Example: deploy-job watchdog 1.0.3"
    exit
fi

if [ -z "$2" ]
  then
    echo "Error: no code version supplied"
    echo "Usage: deploy-job <function-name> <code-version>"
    echo "Example: deploy-job watchdog 1.0.3"
    exit
fi

echo "Updating $1 code to $2 ..."
aws lambda update-function-code --function-name $1 --zip-file fileb://target/spacedog-watchdog-$2-bundle.zip --no-publish

if [ "$?" != "0" ]
	then
		exit
fi

echo "Updating $1 description with $2 ..."
aws lambda update-function-configuration --function-name $1 --description "v$2"

if [ "$?" != "0" ]
    then
        exit
fi