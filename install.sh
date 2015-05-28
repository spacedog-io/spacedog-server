#!/bin/sh
#
# Copyright 2015 MagicLAbs SAS. All Rights Reserved.
#

echo Welcome to MagicLabs Server Install

gcloud compute instances create magic-server-1 --image debian-7 --zone europe-west1-d --machine-type g1-small --no-scopes

gcloud compute copy-files server-jre-8u45-linux-x64.gz magic-server-1:~/bin --zone europe-west1-d

gcloud compute copy-files magiclabs-rest-api-0.1.0-SNAPSHOT-bundle.tar.gz magic-server-1:~/bin --zone europe-west1-d

tar xvzf magiclabs-rest-api-0.1.0-SNAPSHOT-bundle.tar.gz

gcloud compute firewall-rules create allow-http --allow tcp:80 tcp:8080 --log-http --project magic-apps --description "Accept inbound connections on ports 80 and 8080"