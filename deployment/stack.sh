#!/bin/bash
# Script for executing update/create stack commands for aws to build or update the various components/stacks of the cloud infrastructure.

projectDirectory=$(pwd)
command=$1
stackName=$2
environment=$3

#echo "Directory: $projectDirectory"

aws cloudformation ${command} --stack-name ${stackName}-${environment} --template-body file://${projectDirectory}/infrastructure/stacks/${stackName}/cloudformation/stack.json --parameters file://${projectDirectory}/infrastructure/stacks/${stackName}/cloudformation/environments/${environment}.json --capabilities CAPABILITY_NAMED_IAM
