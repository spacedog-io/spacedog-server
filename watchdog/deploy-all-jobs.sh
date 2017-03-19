#!/bin/bash

if [ -z $1 ]
then
	echo "Usage: deploy-all-jobs <version>"
	exit -1
fi

version="$1"
lambda=( watchdog snapshot purgelogs )

for name in "${lambda[@]}"
do
	./deploy-job.sh "$name" $version
done
