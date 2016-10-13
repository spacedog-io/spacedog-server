#!/bin/bash
#
# First,
# Use lambda-publish <function-name> <code-version>
# to upload code and publish a new function version.
#
# Second,
# Get the function version from the stdout.
#
# Third,
# Use lambda-prod <function-name> <function-version>
# to point the PROD alias to the right function version.
#
# Functions names:
#   watchdog
#   Purge
#   SnapshotStart
#   SnapshotCheck
#

if [ -z "$1" ]
  then
    echo "Error: no function name supplied"
    echo "Usage: lambda-publish <function-name> <code-version>"
    echo "Example: lambda-publish watchdog 0.18"
    exit
fi

if [ -z "$2" ]
  then
    echo "Error: no code version supplied"
    echo "Usage: lambda-publish <function-name> <code-version>"
    echo "Example: lambda-publish watchdog 0.18"
    exit
fi

echo "Uploading $1 code version $1 to AWS ..."
aws lambda update-function-code --function-name $1 --zip-file fileb://target/spacedog-watchdog-$2-bundle.zip --no-publish

if [ "$?" != "0" ]
	then
		exit
fi

echo "Publishing $1 lambda function ..."
aws lambda publish-version --function-name $1 --description "$1 $2"

if [ "$?" = "0" ]
  then
	echo
	echo "Done."
fi
