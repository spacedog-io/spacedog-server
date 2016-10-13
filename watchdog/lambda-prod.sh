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
    echo "Usage: lambda-prod <function-name> <function-version>"
    echo "Example: lambda-prod watchdog 24"
    exit
fi

if [ -z "$2" ]
  then
    echo "Error: no function version supplied"
    echo "Usage: lambda-prod <function-name> <function-version>"
    echo "Example: lambda-prod watchdog 24"
    exit
fi

echo "Updating PROD alias to $1 lambda function version $2 ..."

aws lambda update-alias --function-name $1 --name PROD --function-version $2

if [ "$?" = "0" ]
  then
	echo
	echo "Done."
fi
