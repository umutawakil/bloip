#!/bin/bash -v

HEALTHCHECK=$1

sleep 180

#Confirm the app can load before signaling success.
# Helper function to be used below
function error_exit
{
  /opt/aws/bin/cfn-signal -e 1 -r \"$1\" 'AutoScalingGroup'
  exit 1
}

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
/opt/aws/bin/cfn-signal -e 0 --stack $2 --resource AutoScalingGroup;