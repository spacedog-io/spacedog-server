#! /bin/bash
if [ ! -f pid ]; then
        echo "No pid file found!"
        exit
fi

sudo kill $(cat pid)
rm pid
echo "Stopping SpaceDog ..."