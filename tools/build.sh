#!/bin/bash

fuser -k 9998/tcp
home=$(dirname $(dirname $(readlink -f $0)))
echo hier: $home

cd $home
cd oba
mvn install

cd $home

cd oba-server
MAVEN_OPTS=-Xmx1024m mvn exec:java &

