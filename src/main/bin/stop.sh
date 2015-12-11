#!/bin/bash

if [ ! -f pid ]
then
	echo "No pid file found!"
	exit -1
fi

echo "Stopping SpaceDog ..."
kill $(cat pid)

if [ $? -eq 0 ]
then
	rm pid
	echo "Done."
else
	echo "Error."
	exit -1
fi