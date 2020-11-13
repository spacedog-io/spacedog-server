#!/bin/bash

version="6.6.1"

mvn install:install-file -Dfile=repository-s3-$version.jar \
	-DgroupId=org.elasticsearch.plugin -DartifactId=repository-s3 \
    -Dversion=$version -Dpackaging=jar

