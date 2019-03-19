#!/bin/bash

version="6.6.1"
elastic="elasticsearch-$version"

cd target
rm $elastic.tgz
curl -o $elastic.tgz https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-$version.tar.gz
rm -r $elastic
tar xvzf $elastic.tgz

# mvn install:install-file -Dfile=target/repository-s3/$plugin.jar \
# 	-DgroupId=org.elasticsearch.plugin -DartifactId=repository-s3 \
#     -Dversion=$version -Dpackaging=jar

