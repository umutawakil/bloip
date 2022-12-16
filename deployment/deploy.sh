#!/bin/bash

#This script pushes the needed application artifacts to the given environment. That includes static assets like js
#the application itself, a redirect node app, configuration files, and a script that sets environment variables
#for the application. By the time you read this a lot of that may change so use this file to work backwards
# and update this documentation to be up to date.

set -e

environment=$1
localProjectFolder=$2
viewVersion=$3

deploymentBucket="bloip-deployment-${environment}"
assetsBucket="bloip-msc-cdn-${environment}"
preDeploymentBucket="bloip-pre-deployment-${environment}"

#Move static secrets and environment config files to deployment bucket. The preDeploymentBucket is populated manually.
# And its contents are not meant to change across deployments, such as password files and configurations not under source control.
aws s3 cp s3://${preDeploymentBucket} s3://${deploymentBucket} --recursive

#Remove old assets from stale versions
aws s3 rm s3://${assetsBucket} --recursive
find . -name "*.DS_Store" -type f -delete

#push UI assets to assets bucket
aws s3 cp ${localProjectFolder}/src/main/resources/static s3://${assetsBucket}/${viewVersion} --recursive --acl public-read

#build application
${localProjectFolder}/gradlew build -x test

#push the key deployment files to the deployment bucket. The EC2 instance the app will run on will initialize itself
# by pulling the artifacts below onto itself, moving them to the propert locations, and running them as needed.
aws s3 cp ${localProjectFolder}/build/libs/*SNAPSHOT.jar s3://${deploymentBucket}/bloip.jar
#aws s3 cp ${localProjectFolder}/src/main/resources/redirect/index.js s3://${deploymentBucket}/index.js
aws s3 cp ${localProjectFolder}/src/main/resources/bloip.conf s3://${deploymentBucket}/bloip.conf
aws s3 cp ${localProjectFolder}/src/main/resources/bloip.service s3://${deploymentBucket}/bloip.service

echo "Artifacts deployed. Ready for application stack create/update command"