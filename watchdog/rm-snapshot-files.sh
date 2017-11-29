#!/bin/bash

repos=( 2017-12 2017-13 2017-14 2017-15 2017-16 2017-17 2017-18 2017-19 )

for repo in "${repos[@]}"
do
	aws s3 rm --recursive s3://spacedog-snapshots/"$repo"
done
