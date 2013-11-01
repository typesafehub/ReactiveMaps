#!/bin/sh

# for these to work you must first `sbt universal:package-zip-tarball`
# and upload it using
# `gsutil cp target/universal/snapapp-1.0-SNAPSHOT.tgz gs://snapapp`

MAX=20
BATCH=10

startSeed1() {
  gcutil addinstance n0001 --disk=n0001,boot --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --kernel=projects/google/global/kernels/gce-v20130813 --disk=akka-logs,mode=read_write --metadata_from_file=startup-script:scripts/startupBackendSeed.sh --service_account_scopes=storage-ro
}

startOneBackendRegionInstance() {
  gcutil addinstance $1 --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --metadata_from_file=startup-script:scripts/startupBackendRegion.sh --service_account_scopes=storage-ro
}

startOneBackendSummaryInstance() {
  gcutil addinstance $1 --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --metadata_from_file=startup-script:scripts/startupBackendSummary.sh --service_account_scopes=storage-ro
}

startOneFrontendInstance() {
  gcutil addinstance $1 --nopersistent_boot_disk --nosynchronous_mode --zone=europe-west1-a --machine_type=n1-standard-2 --image=projects/debian-cloud/global/images/debian-7-wheezy-v20130816 --kernel=projects/google/global/kernels/gce-v20130813 --metadata_from_file=startup-script:scripts/startupFrontend.sh --service_account_scopes=storage-ro
}

gcutil getproject --project=typesafe-akka --cache_flag_values

#startSeed1
startOneBackendRegionInstance n0002
startOneBackendRegionInstance n0003

startOneBackendSummaryInstance n0004
startOneBackendSummaryInstance n0005

startOneFrontendInstance n0006
startOneFrontendInstance n0007
startOneFrontendInstance n0008
startOneFrontendInstance n0009
startOneFrontendInstance n0010

gcutil getproject

