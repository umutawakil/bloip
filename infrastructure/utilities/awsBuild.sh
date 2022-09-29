#!/bin/bash
#Created by Usman Mutawakil
#This script is used to issue a stack create or update request to the AWS Cloudformation service

#Ensure the script aborts on any errors
set -e

awsCommand=$1
env=$2
stackName=$3

#update the cloudformation properties file with the git commit number and configuration version. These values come from 2 separate github repositories.
#./deployment/utilities/awsParameterInjector.sh ChangeId $CODEBUILD_SOURCE_VERSION deployment/stacks/$stackName/cloudformation/environments/$env.parameters.json

#execute stack creation/update commands
case "$awsCommand" in
	create)
		#Issue stack creation command
		aws cloudformation create-stack --stack-name $stackName --tags Key=Name,Value=$stackName,Key=Owner,Value=usman_mutawakil --template-body file://$CODEBUILD_SRC_DIR/deployment/stacks/$stackName/cloudformation/stack.json --parameters file://$CODEBUILD_SRC_DIR/deployment/stacks/$stackName/cloudformation/environments/$env.parameters.json --capabilities CAPABILITY_NAMED_IAM --disable-rollback
		;;
	update)
		#Issue stack update command
		aws cloudformation update-stack --stack-name $stackName --template-body file://$CODEBUILD_SRC_DIR/deployment/stacks/$stackName/cloudformation/stack.json --parameters file://$CODEBUILD_SRC_DIR/deployment/stacks/$stackName/cloudformation/environments/$env.parameters.json  --capabilities CAPABILITY_NAMED_IAM
		;;
	*)
		echo "invalid command parameter for aws command"
		exit 1
		;;
esac
