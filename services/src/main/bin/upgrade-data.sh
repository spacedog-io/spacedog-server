#!/bin/bash
if [ -f pid ]
then
	echo "Error: pid file exists: check that server is shutdown!"
	exit -1
fi

echo "SpaceDog is about to upgrade elastic data."
echo "Enter root password if you are sure to proceed."

sudo -k echo "Upgrading ..."

if [ $? -eq 0 ]
then	
	java -d64 -cp "lib/*" io.spacedog.services.UpgradeElasticData
else
	echo "Aborted."
	exit -1
fi
