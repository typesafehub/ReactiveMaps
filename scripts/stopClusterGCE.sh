#!/bin/sh

MAX=20

stopInstances() {
  for ((j=$1; j<=MAX; j++)); do
  	local N=`printf "%04d\n" "$j"`
  	echo "Stopping instance n$N"
    /opt/gcutil-1.8.4/gcutil deleteinstance n$N --zone=europe-west1-a --nodelete_boot_pd --force --nosynchronous_mode
  done
}

stopInstances 2

echo "n0001 not stopped, it can be stopped with '/opt/gcutil-1.8.4/gcutil deleteinstance n0001 --zone=europe-west1-a --nodelete_boot_pd --force'"



