import { WASM } from "../../wasm/wasmWrapper.js";

/** Discussion Topic State**/
export var discussionTopic = new (function() {
    this.getName = function() {
        return "discussion-topic";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {

            $("#discussion-topic-form").submit(function(event) {
                event.preventDefault();
                return false;
            });

            $("#discussion-topic-submit-button").click(function() {
                stateMachine.next();
            });
            $("#discussion-topic").keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if(keycode === 13) {
                    stateMachine.next();
                }
            })
        });
    };

    this.show = function() {
        $("#discussion-topic-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#discussion-topic-state-view").css("display", "none");
    };
})();

/** Discussion Info State**/
export var discussionInfo = new (function() {
    this.getName = function() {
        return "discussion-info";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {

            $("#discussion-title-form").submit(function(event) {
                event.preventDefault();
                return false;
            });

            $("#discussion-info-submit-button").click(function() {
                if($("#discussion-title").val() !== "") {
                    stateMachine.next();
                } else {
                    alert("Please enter a title for the conversation");
                }
            });
            $("#discussion-title").keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if(keycode === 13) {
                    stateMachine.next();
                }
            })
        });
    };

    this.show = function() {
        $("#discussion-info-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#discussion-info-state-view").css("display", "none");
    };
})();

/** Idle State**/
export var idleState = new (function() {
    this.getName = function() {
        return "idle";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#record-button").click(function() {
                stateMachine.next();
            });
            WASM.getAudioPermission();
        });
    };

    this.show = function() {
        $("#idle-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#idle-state-view").css("display", "none");
    };
})();

var DURATION = 0;

/** Recording State **/
export var recordingState = new (function() {
    var MAX_COUNT = 60;
    var counter = MAX_COUNT;
    var interval;

    this.getName = function() {
        return "recording";
    };

    this.initEvents = function (stateMachine) {
        $(document).ready(function () {
            $("#stop-button").click(function () {
                stateMachine.next();
            });
        });
    };

    this.show = function (stateMachine) {
        $("#recording-state-view").css("display", "block");
        $("#counter-display").text(counter);

        interval = setInterval(function () {
                counter--;
                $("#counter-display").text(counter);
                if (counter <= 0) {
                    stateMachine.next();
                }
            },
            1000
        );
    };

    this.run = function() {
        $(document).ready(function() {
            WASM.init();
            setTimeout(function () {
                WASM.startRecording();
            }, 500);;
        });
    }

    this.hide = function () {
        $("#recording-state-view").css("display", "none");
        clearInterval(interval);
        DURATION = MAX_COUNT - counter;
        counter = MAX_COUNT;

        WASM.stopRecording();
    };
})();

/** Recording Complete State**/
export var recordingCompleteState = new (function() {
    this.getName = function() {
        return "recording-complete";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#save-button").click(function() {
                stateMachine.next();
            });
            $("#delete-button").click(function() {
                stateMachine.back(3);
            });
        });
    };

    this.show = function() {
        $("#recording-complete-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#recording-complete-state-view").css("display", "none");
    };
})();

/** Creating State**/
export var creatingState = new (function() {
    this.getName = function() {
        return "creating";
    };

    this.initEvents = function(stateMachine) {};

    this.show = function() {
        $("#creating-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        createDiscussion(stateMachine);
    };

    this.hide = function() {
        $("#creating-state-view").css("display", "none");
    };
})();

function createDiscussion(stateMachine) {
    sendRequestForCDNInfo().then((cdninfo) => {
        return uploadFormToCDN(cdninfo);
    }).then(() => {
        sendDiscussionCreationRequest(stateMachine);
    });
}
function sendRequestForCDNInfo() {
    return new Promise(function(resolve, reject) {
        $.ajax({
            type: "get",
            url: "/cdn-info",
            contentType: false,
            processData: false,
            error: function (xhr, textStatus, error) {
                alert("Failed to get information for upload: " + textStatus + " " + error);
                reject();
            },
            success: function (cdninfo) {
                resolve(cdninfo)
            }
        });
    });
}

function uploadFormToCDN(cdninfo) {
    const formData = new FormData();
    formData.append("key", "${filename}");
    formData.append("acl", "public-read");
    formData.append("success_action_redirect", cdninfo.redirectUrl);

    formData.append("Content-Type", "audio/mpeg");
    formData.append("x-amz-meta-uuid","14365123651274");
    formData.append("x-amz-server-side-encryption", "AES256");
    formData.append("x-amz-credential", cdninfo.credential);
    formData.append("x-amz-algorithm", "AWS4-HMAC-SHA256");
    formData.append("x-amz-date", cdninfo.date);

    formData.append("policy", cdninfo.policy);
    formData.append("x-amz-signature", cdninfo.signature);
    formData.append("file", WASM.blob, cdninfo.fileName);

    return new Promise(function(resolve, reject) {
        $.ajax({
            type: "POST",
            url: cdninfo.audioCdnUploadUrl,
            data: formData,
            contentType: false,
            processData: false,
            error: function (xhr, textStatus, error) {
                console.log("Failed to get information for upload: " + textStatus + " " + error);
                //TODO: This is temporary since we don't expect the redirect to do anything.
                resolve();
            },
            success: function () {
                resolve()
            }
        });
    });
}

function sendDiscussionCreationRequest(stateMachine) {
    var formData = new FormData();
    formData.append("title", $("#discussion-title").val());
    formData.append("topicId", $("#discussion-topic").val());
    formData.append("duration", DURATION);

    $.ajax({
        type: "post",
        url: "/discussion-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            alert("Failed to create new discussion: " + textStatus + " " + error);
        },
        success: function (url) {
            stateMachine.next(url);
        }
    });
}

/** Replying State**/
export var replyingState = new (function() {
    this.getName = function() {
        return "replying";
    };

    this.initEvents = function(stateMachine) {};

    this.show = function() {
        $("#replying-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        createReply(stateMachine);
    };

    this.hide = function() {
        $("#replying-state-view").css("display", "none");
    };
})();

function createReply(stateMachine) {
    sendRequestForCDNInfo().then((cdninfo) => {
        return uploadFormToCDN(cdninfo);
    }).then(() => {
        sendReplyCreationRequest(stateMachine);
    });
}

function sendReplyCreationRequest(stateMachine) {
    var formData = new FormData();
    formData.append("discussionId", parseInt($("#reply-discussion-id").text()));
    formData.append("duration", DURATION);

    $.ajax({
        type: "post",
        url: "/reply-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            alert("Failed to reply to discussion. " + textStatus + " " + error);
        },
        success: function (url) {
            stateMachine.next(url);
        }
    });
}

/** Confirmation State**/
export var discussionConfirmationState = new (function() {
    var discussionUrl;

    this.getName = function() {
        return "discussion-confirmation";
    };

    this.init = function(stateMachine, previousStateData) {
        discussionUrl = previousStateData
    }

    this.initEvents = function() {};

    this.show = function(stateMachine, discussionUrl) {
        $("#discussion-confirmation-state-view").css("display", "block");
        $("#discussion-confirmation-title").html($("#discussion-title").val());
        $("#discussion-confirmation-url").text(discussionUrl);
        $("#discussion-confirmation-url").attr("href", discussionUrl);
    };

    this.run = function() {
        //TODO: This is the one of two areas (see the other call of this function below) that when enabled will allow the front-end to start subscribing for pushnotifications
        //TODO: On the back-end you just need to renable the push notifications job. This will all most likely
        //TODO: Be replaced by email notifications.
        //explainWebPushNotificationsIfNeeded();
    };
    this.hide = function() {};
})();

export var replyConfirmationState = new (function() {
    var replyUrl;

    this.getName = function() {
        return "reply-confirmation";
    };

    this.init = function(stateMachine, previousStateData) {
        replyUrl = previousStateData
    }

    this.initEvents = function() {};

    this.show = function(stateMachine, replyUrl) {
        $("#reply-confirmation-state-view").css("display", "block");
        $("#reply-confirmation-title").html($("#reply-title").val());
        $("#reply-confirmation-url").text(replyUrl);
        $("#reply-confirmation-url").attr("href", replyUrl);
    };

    this.run = function() {
        //TODO: This is the one of two areas (see the other call of this function above) that when enabled will allow the front-end to start subscribing for pushnotifications
        //TODO: On the back-end you just need to renable the push notifications job. This will all most likely
        //TODO: Be replaced by email notifications.
        //explainWebPushNotificationsIfNeeded();
    };
    this.hide = function() {};
})();