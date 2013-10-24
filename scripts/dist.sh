#!/bin/sh

#sbt clean dist
unzip target/universal/snapapp-1.0-SNAPSHOT.zip -d target
mv target/snapapp-1.0-SNAPSHOT target/snapapp
cp scripts/* target/snapapp/bin/
tar -cz -C target -f target/snapapp.tgz snapapp


