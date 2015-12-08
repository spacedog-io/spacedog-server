#! /bin/bash
if [ -f pid ]; then
        echo "Error: pid file already exists!"
        exit
fi

sudo java -d64 -cp "libs/*" io.spacedog.services.Start $HOME &>log &
echo $! > pid

echo "SpaceDog is starting ..."
echo "Check pid file for  process id."
echo "Check log file for logs (tail -f log)."