#!/bin/bash

version="6.4.3"
plugin="repository-s3-$version"

curl -o target/$plugin.zip https://artifacts.elastic.co/downloads/elasticsearch-plugins/repository-s3/$plugin.zip

unzip -u target/$plugin.zip -d target/repository-s3

mvn install:install-file -Dfile=target/repository-s3/$plugin.jar \
	-DgroupId=org.elasticsearch.plugin -DartifactId=repository-s3 \
    -Dversion=$version -Dpackaging=jar