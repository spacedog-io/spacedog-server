#!/bin/bash
if [ -f pid ]
then
	echo "Error: pid file already exists!"
	exit -1
fi

echo "SpaceDog is starting ..."
java -d64 -cp "lib/*" io.spacedog.services.Start &>log &

if [ $? -eq 0 ]
then
	echo $! > pid
	echo "Check pid file for  process id."
	echo "Check log file (tail -f log)."
else
	echo "Error starting SpaceDog."
	exit -1
fi
