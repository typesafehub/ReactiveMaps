#! /bin/bash

VERSION=1.0-SNAPSHOT

gsutil cp gs://jvm/jdk-7u40-linux-x64.tar.gz - | tar -C /opt -xzf -

cd /tmp
gsutil cp gs://snapapp/snapapp-$VERSION.tgz - | tar xzf -
gsutil cp -R gs://snapapp/patches .
cd snapapp-$VERSION

#patch it
cp -a ../patches/* .

exec bash scripts/startBackendSeed.sh
