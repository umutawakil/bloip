
WebAudioRecorder  = function() {
    /*var mediaRecorder;
    var chunks = [];
    var blob;
    var stream;*/

    this.getBlob = function(){
        return this.blob;
    };
    this.startRecording = function() {
        var me = this;
        navigator.mediaDevices.getUserMedia({audio: true}).then(function(stream) {
            me.chunks                        = [];
            me.stream                        = stream;
            me.mediaRecorder                 = new MediaRecorder(stream, {mimeType: AudioType});
            me.mediaRecorder.ondataavailable = function(e) { /** This only runs once **/
                me.chunks.push(e.data);
            };
            me.mediaRecorder.onstop = function () {
                try {
                    me.blob = new Blob(me.chunks, {type: AudioType});
                    document.getElementById("previewControl").type = AudioType;
                    document.getElementById("previewControl").src  = window.URL.createObjectURL(me.getBlob());

                } catch (exception) {
                    logUserEvent("Error-recording-mediaRecorder.onstop", exception.message);
                    alert($("#t-error-read-save-microphone").text());
                    window.location.href = "/";
                }
            };

        }).then(function() {
            try {
                me.mediaRecorder.start();
            } catch (startException) {
                console.error(startException);
                logUserEvent("Error-recording-mediaRecorder.start", startException.message);
                alert($("#t-error-start").text());
                window.location.href = "/";
            }
        }).catch(function (error) {
            logUserEvent("Error-recording-getUserMedia.catch",  error.message);
            alert($("#t-error-start").text());
            window.location.href = "/";
        });
    };
    this.stopRecording = function() {
        try {
            this.mediaRecorder.stop();
            this.closeStream(this.stream);
        } catch (e) {
            logUserEvent("Error-recording-mediaRecorder.stop",  e.message);
            alert($("#t-error-stop").text());
            window.location.href = "/";
        }
    };
    this.closeStream = function(x) {
        x.getTracks().forEach(track => track.stop());
    };
    this.getAudioPermission = function(stateMachine) {
        const userMedia = navigator.mediaDevices.getUserMedia({audio: true}).catch(function(error) {
            console.log(error);
            logUserEvent("Error-recording-getAudioPermission.getUserMedia", error.message);
            alert($("#t-microphone-required").text() + " (1)");
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
            logUserEvent("Error-recording-getAudioPermission.getUserMedia.then",  error.message);
            alert($("#t-error-access").text() + " (2)");
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
            window.location.href = "/";
            return;
        }

        const doublePost = document.getElementById("double-post");
        if (doublePost && doublePost.textContent === "yes") {
            const message2 = document.getElementById("t-double-post").textContent;
            alert(message2);
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
        if (localStorage.getItem("microphone") === "asked") {
            webAudioRecorder.getAudioPermission(stateMachine);
            return;
        }
        if (confirm($("#t-microphone-access-request").text())) {
            webAudioRecorder.getAudioPermission(stateMachine);
        } else {
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
        setTimeout(function () {
            webAudioRecorder.startRecording();
        }, 500);
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
        createDiscussion(stateMachine);
    };

    this.hide = function() {
        $("#creating-state-view").css("display", "none");
    };
})();

function createDiscussion(stateMachine) {
    sendRequestForCDNInfo().then((cdninfo) => {
        if(cdninfo.censored) {
            return false
        } else {
            return uploadFormToCDN(cdninfo);
        }
    }).then((result) => {
        if(result === false) {
            return
        }
        sendDiscussionCreationRequest(stateMachine);

    }).catch(function(error){
        console.error(error);
        logUserEvent("Error-recording-createDiscussion",  error.message);
        alert($("#t-error-create").text());
    });
}
function sendRequestForCDNInfo() {
    return new Promise(function(resolve, reject) {
        $.ajax({
            type: "POST",
            url: "/cdn-info",
            contentType: false,
            processData: false,
            error: function (xhr, textStatus, errorThrown) {
                logUserEvent("Error-recording-sendRequestForCDNInfo", "HTTP: " + xhr.status + ", text_status: " + textStatus +", error: " + errorThrown);
                alert($("#t-error-communication").text() +" (r-info)");
                window.location.href = "/";
                //reject(errorThrown);
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
            error: function (xhr, textStatus, errorThrown) {
                logUserEvent("Error-recording-uploadFormToCDN",  "HTTP: " + xhr.status +", text_status: " + textStatus +", error: " + errorThrown);
                alert($("#t-error-communication").text() +" (upload)");
                window.location.href = "/";
                //resolve(true);
            },
            success: function (fileUploadResponse) {
                console.log("Direct file upload response: " + fileUploadResponse);
                resolve(fileUploadResponse)
            }
        });
    });
}

function sendDiscussionCreationRequest(stateMachine) {
    var formData = new FormData();
    var youtubeLink = $("#discussion-video").val();

    formData.append("title", $("#discussion-title").val());
    if (youtubeLink.length > 0) {
        formData.append("youtubeLink", youtubeLink);
    }
    //formData.append("topicId", $("#discussion-topic").val());
    formData.append("duration", DURATION);

    $.ajax({
        type: "POST",
        url: "/discussion-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, errorThrown) {
            logUserEvent("Error-recording-sendDiscussionCreationRequest",  "HTTP: " + xhr.status +", text_status: " + textStatus +", error: " + errorThrown);
            alert($("#t-error-communication").text() +" (SDCR)");
            window.location.href = "/";
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
        createReply(stateMachine);
    };

    this.hide = function() {
        $("#replying-state-view").css("display", "none");
    };
})();

function createReply(stateMachine) {
    sendRequestForCDNInfo().then((cdnInfo) => {
        if (cdnInfo.censored) {
            return false;
        } else {
            return uploadFormToCDN(cdnInfo);
        }
    }).then((result) => {
        if (result === false) {
            return;
        }
        sendReplyCreationRequest(stateMachine);

    }).catch(function (error) {
        console.log(error);
        logUserEvent("Error-recording-createReply",  error.message);
        alert($("#t-error-reply").text());
    });
}

function sendReplyCreationRequest(stateMachine) {
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
        error: function (xhr, textStatus, errorThrown) {
            logUserEvent("Error-recording-sendReplyCreationRequest",  "HTTP: " + xhr.status +", text_status: " + textStatus +", error: " + errorThrown);
            alert($("#t-error-communication").text()+" (SRCR)");
            window.location.href = "/";
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
        $("#discussion-confirmation-url").text(discussionUrl);
        $("#discussion-confirmation-url").attr("href", discussionUrl);
    };

    this.run = function() {
        globalStateMachine.complete();
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
        $("#reply-confirmation-url").text(replyUrl);
        $("#reply-confirmation-url").attr("href", replyUrl);
    };

    this.run = function() {
        globalStateMachine.complete();
    };
    this.hide = function() {};
})();

function checkForMicrophoneSdkAvailability() {
    if (doestNotExist(navigator.mediaDevices)) {
        logUserEvent("Error-recording-checkForMicrophoneSdkAvailability", "navigator.mediaDevices does not exist");
        alert($("#t-no-browser-support").text() +"\r\n" + $("#t-upgrade-message").text() +"\r\n" + "(error: microphone support)");
        window.location.href = "/";
        return;
    }
    if (doestNotExist(MediaRecorder)) {
        logUserEvent("Error-recording-checkForMicrophoneSdkAvailability", "MediaRecorder does not exist");
        alert($("#t-no-browser-support").text() +"\r\n" + $("#t-upgrade-message").text() +"\r\n" + "(error: media support)");
        window.location.href = "/";
        return;
    }
}

function doestNotExist(input) {
    if (typeof  input === "undefined") {
        return true;
    }
    return false;
}