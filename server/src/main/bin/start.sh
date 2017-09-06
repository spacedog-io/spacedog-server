#!/bin/bash
if [ -f pid ]
then
	echo "Error: pid file already exists!"
	exit -1
fi

echo "SpaceDog is starting ..."
java -d64 -cp "lib/*" io.spacedog.server.Start &>log &

if [ $? -eq 0 ]
then
	echo $! > pid
	echo "Check pid file for process id."
else
	echo "Error starting SpaceDog."
	exit -1
fi

echo "Tailing the log ..."
tail -f log
