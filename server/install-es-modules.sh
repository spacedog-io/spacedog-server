#!/bin/bash

version="6.6.1"

mvn install:install-file -Dfile=modules/repository-s3/repository-s3-$version.jar \
    -DgroupId=org.elasticsearch.plugin -DartifactId=repository-s3 \
    -Dversion=$version -Dpackaging=jar

mvn install:install-file -Dfile=modules/transport-netty4/transport-netty4-$version.jar \
    -DgroupId=org.codelibs.elasticsearch.module -DartifactId=transport-netty4 \
    -Dversion=$version -Dpackaging=jar

mvn install:install-file -Dfile=modules/analysis-common/analysis-common-$version.jar \
    -DgroupId=org.codelibs.elasticsearch.module -DartifactId=analysis-common \
    -Dversion=$version -Dpackaging=jar

mvn install:install-file -Dfile=modules/reindex/reindex-$version.jar \
    -DgroupId=org.codelibs.elasticsearch.module -DartifactId=reindex \
    -Dversion=$version -Dpackaging=jar
