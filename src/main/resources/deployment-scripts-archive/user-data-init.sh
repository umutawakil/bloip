#!/bin/bash -v 
set -e
#Echo the changeId just so that changes to the changeId can drive an update on the autoscaling group
cd /
echo \"",{"Ref": "ChangeId"},"

deploymentBucket=$1
stackName=$2
healthcheckUrl=$3

#Download the deployment artifacts
aws s3 cp s3://$deploymentBucket ./ --recursive --exclude \"*/*\"

#Setup log rotation
mv bloip.conf /etc/logrotate.d
logrotate /etc/logrotate.d/bloip.conf

#install bloip as a service
mv bloip.service bloip
chmod +x bloip
mv bloip /etc/init.d
chkconfig --add bloip

#Maybe send this to devnull
service bloip start

#Wait 3 minutes for the app to be ready
sleep 180

#Confirm the app can load before signaling success.
# Helper function to be used below
function error_exit
{
  /opt/aws/bin/cfn-signal -e 1 -r "$1" 'AutoScalingGroup'
  exit 1
}

HEALTHCHECK=$healthcheckUrl
if [[ $HEALTHCHECK =~ (.*):(.*) ]]; then
PROTOCOL=${BASH_REMATCH[1]}
URI=${BASH_REMATCH[2]}
else
error_exit 'Invalid healthcheck URL'
fi
URL="${PROTOCOL}://localhost:${URI}"
echo "healthcheck url is $URL"
status=`curl -k -s -w "%{http_code}" -m 10 $URL -o /dev/null`
until [ $status == '200' ]; do
  sleep 10
  status=`curl -k -s -w "%{http_code}" -m 10 $URL -o /dev/null`
  echo "Got $status"
  if [[ $status != '000' && $status != '200' ]]; then
      error_exit 'Deploy failed'
   fi
done

# All done so signal success back to AWS to complete the stack creation/update process
/opt/aws/bin/cfn-signal -e 0 --stack $stackName --resource AutoScalingGroup;