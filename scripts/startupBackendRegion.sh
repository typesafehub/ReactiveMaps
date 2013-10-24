#! /bin/bash

sudo mkdir /mnt/akka-testapp
sudo /usr/share/google/safe_format_and_mount -m "mkfs.ext4 -F" /dev/disk/by-id/google-akka-testapp /mnt/akka-testapp

# CentOS
#sudo yum install -y java-1.7.0-openjdk.x86_64
# Debian
#sudo apt-get -y install openjdk-7-jre

sudo tar -C /opt -xf /mnt/akka-testapp/install/jdk-7u40-linux-x64.tar

cd /mnt/akka-testapp/
bin/startBackendRegion.sh

