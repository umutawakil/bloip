//var AudioType = "audio/mp4";

WebAudioRecorder  = function() {
    var mediaRecorder;
    var chunks = [];
    var blob;
    var stream;

    this.getBlob = function(){
        /*if (typeof self.blob != "undefined") {
            return self.blob;
        }
        self.blob = new Blob(self.chunks, { type : 'audio/webm; codecs=opus'});
        console.log("New blob: " + self.blob.length);*/

        return this.blob;
    };
    this.startRecording = function() {
        var me = this;
        navigator.mediaDevices.getUserMedia({audio: true}).then(function(stream) {
            me.chunks = [];
            me.stream = stream;
            me.mediaRecorder = new MediaRecorder(stream, {mimeType: AudioType});
            /** Important to note this only runs once when the whole file is ready **/
            me.mediaRecorder.ondataavailable = function(e) {
                me.chunks.push(e.data);
            };
            me.mediaRecorder.onstop = function () {
                try {
                    me.blob = new Blob(me.chunks, {type: AudioType});
                    document.getElementById("previewControl").type = AudioType;
                    document.getElementById("previewControl").src  = window.URL.createObjectURL(me.getBlob());

                } catch (exception) {
                    alert("Error: " + exception);
                    logUserEvent("error", "startRecording", "recording",exception.message, 1);
                }
            };

        }).then(function() {
            me.mediaRecorder.start();

        }).catch(function (error){
            console.log(error);
            alert("Error: Something happened while trying record from your microphone. " + error.message);
            logUserEvent("error", "startRecording.catch", "recording", error.message, 1);
            window.location.href = "/";

        });
    };
    this.stopRecording = function() {
        try {
            this.mediaRecorder.stop();
            this.closeStream(this.stream);
        } catch (exception) {
            alert("Error: " + exception);
            logUserEvent("error", "stopRecording", "recording", exception.message, 1);
        }
    };
    this.closeStream = function(x) {
        x.getTracks().forEach(track => track.stop());
    };
    this.getAudioPermission = function(stateMachine) {
        const userMedia = navigator.mediaDevices.getUserMedia({audio: true}).catch(function(error) {
            console.log(error);
            document.getElementById("").
            alert($("#t-microphone-required").text());
            logUserEvent("error", "getAudioPermissionA", "recording", error.message, 1);
            window.location.href = "/";
        });

        var me = this;
        userMedia.then(function(stream) {
            console.log("Audio permission verified. Closing temporary stream.");
            localStorage.setItem("microphone","asked");
            me.closeStream(stream);
            stateMachine.next();

        }).catch(function(error) {
            console.log(error);
            alert("Error: Something happened while trying to open your microphone. Send us this message on twitter: " + error.message);
            logUserEvent("error", "getAudioPermissionB","recording", error.message, 1);
        });
    };
}

/** Verify the discussion creation limit is not reached
 *  or that the user is NOT double replying **/
var limitChecks = new (function() {
    this.getName = function() {
        return "limit-checks";
    };
    this.initEvents = function(stateMachine) {};
    this.show = function() {};

    this.run = function(stateMachine) {
        const createLimit = document.getElementById("create-limit");
        if (createLimit && createLimit.textContent === "yes") {
            const message1 = document.getElementById("t-creation-limit-day").textContent
            alert(message1);
            logUserEvent("discussion_creation_limit_reached", "limitChecks.run", "recording","", 1);
            window.location.href = "/";
            return;
        }

        const doublePost = document.getElementById("double-post");
        if (doublePost && doublePost.textContent === "yes") {
            const message2 = document.getElementById("t-double-post").textContent;
            alert(message2);
            logUserEvent("double_post_blocked", "limitChecks.run", "recording", "", 1);
            window.location.href = "/";
            return;
        }

        stateMachine.next();
    };

    this.hide = function() {};
})();


const webAudioRecorder = new WebAudioRecorder();
/** Audio permission State**/
var microphonePermission = new (function() {
    this.skipOnBackButton = true;

    this.getName = function() {
        return "microphone-permission";
    };

    this.initEvents = function(stateMachine) {
       checkForMicrophoneSdkAvailability();
    };

    this.show = function() {
        $("#microphone-permission-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        logUserEvent("microphone_permission", "microphonePermission.run", "recording", "", 0);

        if (localStorage.getItem("microphone") === "asked") {
            webAudioRecorder.getAudioPermission(stateMachine);
            return;
        }
        if (confirm($("#t-microphone-access-request").text())) {
            webAudioRecorder.getAudioPermission(stateMachine);
        } else {
            logUserEvent("error", "microphonePermission.run", "recording", "microphone rejected", 1);
            window.location.href = "/";
        }
    };

    this.hide = function() {
        $("#microphone-permission-state-view").css("display", "none");
    };

})();

/** Discussion Topic State**/
var discussionTopic = new (function() {
    this.getName = function() {
        return "discussion-topic";
    };

    this.initEvents = function(stateMachine) {
        checkForMicrophoneSdkAvailability();

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
var discussionInfo = new (function() {
    this.getName = function() {
        return "discussion-info";
    };

    this.initEvents = function(stateMachine) {
        $("#discussion-title-form").submit(function(event) {
            event.preventDefault();
            return false;
        });

        $("#discussion-info-submit-button").click(function() {
            if($("#discussion-title").val() !== "") {
                stateMachine.next();
                logUserEvent("discussion_title_chosen", "discussionInfo.title", "recording", "", 0);
            } else {
                alert($("#t-title-missing").text());
            }
        });
        $("#discussion-title").keypress(function(event) {
            var keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode === 13) {
                stateMachine.next();
            }
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

/** Discussion video State**/
var discussionVideo = new (function() {
    this.getName = function() {
        return "discussion-video";
    };

    this.initEvents = function(stateMachine) {
        $("#discussion-video-form").submit(function(event) {
            event.preventDefault();
            return false;
        });

        $("#discussion-video-submit-button").click(function() {
            if($("#discussion-video").val() == "" || isValidHttpUrl($("#discussion-video").val())) {
                stateMachine.next();
            } else {
                alert($("#t-bad-youtube-link").text());
            }
        });
        $("#discussion-video").keypress(function(event) {
            var keycode = (event.keyCode ? event.keyCode : event.which);
            if (keycode === 13) {
                stateMachine.next();
            }
        });
    };

    this.show = function() {
        $("#discussion-video-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#discussion-video-state-view").css("display", "none");
    };
})();

function isValidHttpUrl(string) {
    var url;
    try {
        url = new URL(string);
    } catch (_) {
        return false;
    }
    return (url.protocol === "http:" || url.protocol === "https:")
        && (
            url.hostname == "youtube.com" || url.hostname == "youtu.be"
            || url.hostname == "www.youtube.com" || url.hostname === "www.youtu.be"
            || url.hostname == "m.youtube.com" || url.hostname == "m.youtu.be"
        );
}

/** Idle State**/
var idleState = new (function() {
    this.getName = function() {
        return "idle";
    };

    this.initEvents = function(stateMachine) {
        $("#record-button").click(function() {
            stateMachine.next();
        });
    };

    this.show = function() {
        $("#idle-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        logUserEvent("idle_state", "idleState.run", "recording", "", 0);
    };

    this.hide = function() {
        $("#idle-state-view").css("display", "none");
    };
})();

var DURATION = 0;

/** Recording State **/
var recordingState = new (function() {
    this.skipOnBackButton = true;
    var MAX_COUNT = 60;
    var counter = MAX_COUNT;
    var interval;

    this.getName = function() {
        return "recording"; //This name is tied to the backbutton/history logic in the popstate event handler
    };

    this.initEvents = function (stateMachine) {
        $("#stop-button").click(function () {
            stateMachine.next();
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
        //WASM.init();
        /*setTimeout(function () {
            WASM.startRecording();
        }, 500);*/
        setTimeout(function () {
            webAudioRecorder.startRecording();
        }, 500);
        logUserEvent("recording", "recordingState.run", "recording","", 0);
    }

    this.hide = function () {
        $("#recording-state-view").css("display", "none");
        clearInterval(interval);
        DURATION = MAX_COUNT - counter;
        counter = MAX_COUNT;

        webAudioRecorder.stopRecording();
    };
})();

/** Recording Complete State**/
var recordingCompleteState = new (function() {
    this.getName = function() {
        return "recording-complete";
    };

    this.initEvents = function(stateMachine) {
        $("#save-button").click(function() {
            stateMachine.next();
        });
        $("#delete-button").click(function() {
            if(confirm($("#t-delete-warning").text())) {
                stateMachine.back(3);
            }
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
var creatingState = new (function() {
    this.getName = function() {
        return "creating";
    };

    this.initEvents = function(stateMachine) {};

    this.show = function() {
        $("#creating-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        logUserEvent("discussion_creation", "creatingState.run", "recording","", 0);
        createDiscussion(stateMachine);
    };

    this.hide = function() {
        $("#creating-state-view").css("display", "none");
    };
})();

function createDiscussion(stateMachine) {
    sendRequestForCDNInfo().then((cdninfo) => {
        if(cdninfo.censured) {
            return false
        } else {
            return uploadFormToCDN(cdninfo);
        }
    }).then((fileUploadResponse) => {
        if(fileUploadResponse !== false) {
            sendDiscussionCreationRequest(stateMachine); //TODO: Should this be a promise?
        }
    }).catch(function(error){
        console.log(error);
    });
}
function sendRequestForCDNInfo() {
    return new Promise(function(resolve, reject) {
        $.ajax({
            type: "POST",
            url: "/cdn-info",
            contentType: false,
            processData: false,
            error: function (xhr, textStatus, error) {
                alert("Failed to get information for upload: " + textStatus + " " + error +". Send me this message on Twitter so I can fix this bug.");
                logUserEvent("error", "sendRequestForCDNInfo", "recording", "", 1);
                reject(error);
            },
            success: function (cdninfo) {
                logUserEvent("cdn_info_request", "sendRequestForCDNInfo", "recording", "", 0);
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

    formData.append("Content-Type", AudioType);
    formData.append("x-amz-meta-uuid","14365123651274");
    formData.append("x-amz-credential", cdninfo.credential);
    formData.append("x-amz-algorithm", "AWS4-HMAC-SHA256");
    formData.append("x-amz-date", cdninfo.date);

    formData.append("policy", cdninfo.policy);
    formData.append("x-amz-signature", cdninfo.signature);
    formData.append("file", webAudioRecorder.getBlob(), cdninfo.fileName);

    return new Promise(function(resolve, reject) {
        $.ajax({
            type: "POST",
            url: cdninfo.audioCdnUploadUrl,
            data: formData,
            contentType: false,
            processData: false,
            error: function (xhr, textStatus, error) {
                console.log("Failed to get information for upload: " + textStatus + " " + error +". Send this error message to me on Twitter so I can fix this bug.");
                logUserEvent("error", "uploadFormToCDN", "recording", error, 1);
                reject(error);
            },
            success: function (fileUploadResponse) {
                console.log("Direct file upload response: " + fileUploadResponse);
                logUserEvent("upload_to_cdn", "uploadFormToCDN", "recording", "", 0);
                resolve(fileUploadResponse)
            }
        });
    });
}

function sendDiscussionCreationRequest(stateMachine) {
    logUserEvent("send_discussion_creation_request", "sendDiscussionCreationRequest", "recording","", 0);

    var formData = new FormData();
    var youtubeLink = $("#discussion-video").val();

    formData.append("title", $("#discussion-title").val());
    if (youtubeLink.length > 0) {
        formData.append("youtubeLink", youtubeLink);
    }
    //formData.append("topicId", $("#discussion-topic").val());
    formData.append("duration", DURATION);
    formData.append("eventSequenceId", document.body.getAttribute("event-sequence-id"));

    $.ajax({
        type: "POST",
        url: "/discussion-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            console.error(error);
            alert("Failed to create new discussion: " + textStatus + " " + error + ". Send me a message via Twitter so I can fix this bug.");
            logUserEvent("error", "sendDiscussionCreationRequest", "recording", error, 1);
        },
        success: function (url) {
            stateMachine.next(url);
        }
    });
}

/** Replying State**/
var replyingState = new (function() {
    this.getName = function() {
        return "replying";
    };

    this.initEvents = function(stateMachine) {};

    this.show = function() {
        $("#replying-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        logUserEvent("reply_creation", "replyingState.run", "recording", "", 0);
        createReply(stateMachine);
    };

    this.hide = function() {
        $("#replying-state-view").css("display", "none");
    };
})();

function createReply(stateMachine) {
    sendRequestForCDNInfo().then((cdnInfo) => {
        if (cdnInfo.censured) {
            return cdnInfo;
        }else {
            return uploadFormToCDN(cdnInfo);
        }
    }).then((result) => {
        if(result.censured) {
            return
        } else {
            sendReplyCreationRequest(stateMachine); //TODO: Should this be a promise?
        }
    }).catch(function (error) {
        console.log(error);
    });
}

function sendReplyCreationRequest(stateMachine) {
    logUserEvent("send_reply_creation_request", "sendReplyCreationRequest", "recording","", 0);

    var formData = new FormData();
    formData.append("discussionId", parseInt($("#reply-discussion-id").text()));
    formData.append("duration", DURATION);
    formData.append("eventSequenceId", document.body.getAttribute("event-sequence-id"));

    $.ajax({
        type: "post",
        url: "/reply-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            alert("Failed to reply to discussion. " + textStatus + " " + error + ". Send me a message via Twitter so I can fix this bug.");
            logUserEvent("error", "sendReplyCreationRequest", "recording","", 1);
        },
        success: function (url) {
            stateMachine.next(url);
        }
    });
}

/** Confirmation State**/
var discussionConfirmationState = new (function() {
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
        $("#discussion-confirmation-url").text(discussionUrl+"/l/"+$("#language-code").text());
        $("#discussion-confirmation-url").attr("href", discussionUrl+"/l/"+$("#language-code").text());
        logUserEvent("discussion_creation_confirmation", "discussionConfirmationState.show", "recording", "Discussion created!!", 1);
    };

    this.run = function() {
        globalStateMachine.complete();
        //TODO: This is the one of two areas (see the other call of this function below) that when enabled will allow the front-end to start subscribing for pushnotifications
        //TODO: On the back-end you just need to renable the push notifications job. This will all most likely
        //TODO: Be replaced by email notifications.
        //explainWebPushNotificationsIfNeeded();
    };
    this.hide = function() {};
})();

var replyConfirmationState = new (function() {
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
        $("#reply-confirmation-url").text(replyUrl+"/l/"+$("#language-code").text());
        $("#reply-confirmation-url").attr("href", replyUrl+"/l/"+$("#language-code").text());
        logUserEvent("reply_creation_confirmation", "replyConfirmationState.show", "recording", "Reply created!!", 1);
    };

    this.run = function() {
        globalStateMachine.complete();
        //TODO: This is the one of two areas (see the other call of this function above) that when enabled will allow the front-end to start subscribing for pushnotifications
        //TODO: On the back-end you just need to renable the push notifications job. This will all most likely
        //TODO: Be replaced by email notifications.
        //explainWebPushNotificationsIfNeeded();
    };
    this.hide = function() {};
})();

function checkForMicrophoneSdkAvailability() {
    if (doestNotExist(navigator.mediaDevices)) {
        logUserEvent("error", "checkForMicrophoneSdkAvailability", "navigator.mediaDevices does not exist", 1);
        alert($("#t-no-browser-support").text() +"\r\n" + $("#t-upgrade-message").text() +"\r\n" + "(error: microphone support)");
        window.location.href = "/";
        return;
    }
    if (doestNotExist(MediaRecorder)) {
        logUserEvent("error", "checkForMicrophoneSdkAvailability", "recording", "MediaRecorder does not exist", 1);
        alert($("#t-no-browser-support").text() +"\r\n" + $("#t-upgrade-message").text() +"\r\n" + "(error: media support)");
        window.location.href = "/";
        return;
    }
}

function doestNotExist(input) {
    if(typeof  input === "undefined") {
        return true;
    }
    return false;
}