/** Currently this is pasted into the cloud formation in one line **/
const AWS = require('aws-sdk');
var sqs = new AWS.SQS();

exports.handler = function(event) {
    console.log("REQUEST RECEIVED:" + JSON.stringify(event));
    var params = {
        MessageBody: event.detail.jobId,
        QueueUrl: process.env.QUEUE_URL,
    };
    sqs.sendMessage(params, function(err, data) {
        if (err) console.log(err, err.stack);
        else     console.log(data);
    });
};
