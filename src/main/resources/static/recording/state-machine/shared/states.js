/*import { WASM } from "../../wasm/wasmWrapper.js";*/
/*
import AudioRecorder from 'https://cdn.jsdelivr.net/npm/audio-recorder-polyfill/index.js'
import mpegEncoder   from 'https://cdn.jsdelivr.net/npm/audio-recorder-polyfill/mpeg-encoder/index.js'

AudioRecorder.encoder = mpegEncoder
AudioRecorder.prototype.mimeType = 'audio/mpeg'
window.MediaRecorder = AudioRecorder*/

//var AudioType = "audio/mp4";

class WebAudioRecorder {

    constructor() {
        this.mediaRecorder;
        this.chunks = [];
        this.blob;
        this.stream;
    }

    getBlob() {
        /*if (typeof self.blob != "undefined") {
            return self.blob;
        }
        self.blob = new Blob(self.chunks, { type : 'audio/webm; codecs=opus'});
        console.log("New blob: " + self.blob.length);*/

        return this.blob;
    }
    startRecording() {
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
                    alert("ErrorStack1: " + exception.stack);
                    alert("Error2: " + exception);
                }
            };

        }).then(function() {
            me.mediaRecorder.start();

        }).catch(function (error){
            console.log(error);
            alert("Error: Something happened while trying record from your microphone. Send us this message on twitter: " + error.stack);
            window.location.href = "/";
        });
    }
    stopRecording() {
        try {
            this.mediaRecorder.stop();
            this.closeStream(this.stream);
        } catch (exception) {
            alert("ErrorStack: " + exception.stack);
            alert("Error: " + exception);
        }
    }
    closeStream(x) {
        x.getTracks().forEach(track => track.stop());
    }
    getAudioPermission(stateMachine) {
        const userMedia = navigator.mediaDevices.getUserMedia({audio: true}).catch(function(error) {
            console.log(error);
            alert("You need to allow microphone access in order to use your microphone! Check your browser settings for this site.");
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
            alert("Error: Something happened while trying to open your microphone. Send us this message on twitter: " + error.stack);
        });
    }
}
const webAudioRecorder = new WebAudioRecorder();
/** Audio permission State**/
export var microphonePermission = new (function() {
    this.getName = function() {
        return "microphone-permission";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function () {
            checkForMicrophoneSdkAvailability();
        });
    };

    this.show = function() {
        $("#microphone-permission-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        if (localStorage.getItem("microphone") === "asked") {
            webAudioRecorder.getAudioPermission(stateMachine);
            return;
        }
        if (confirm("We need to access your microphone. Is that Okay?")) {
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
export var discussionTopic = new (function() {
    this.getName = function() {
        return "discussion-topic";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
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
            });
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
export var discussionVideo = new (function() {
    this.getName = function() {
        return "discussion-video";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {

            $("#discussion-video-form").submit(function(event) {
                event.preventDefault();
                return false;
            });

            $("#discussion-video-submit-button").click(function() {
                if($("#discussion-video").val() == "" || isValidHttpUrl($("#discussion-video").val())) {
                    stateMachine.next();
                } else {
                    alert("The link you entered is invalid. Make sure you copied it correctly. Youtube only.");
                }
            });
            $("#discussion-video").keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if(keycode === 13) {
                    stateMachine.next();
                }
            });
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
export var idleState = new (function() {
    this.getName = function() {
        return "idle";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#record-button").click(function() {
                stateMachine.next();
            });
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
        return "recording"; //This name is tied to the backbutton/history logic in the popstate event handler
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
            //WASM.init();
            /*setTimeout(function () {
                WASM.startRecording();
            }, 500);*/
            setTimeout(function () {
                webAudioRecorder.startRecording();
            }, 500);
        });
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
                if(confirm("This will permanently delete your recording.")) {
                    stateMachine.back(3);
                }
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
        sendDiscussionCreationRequest(stateMachine); //TODO: Should this be a promise?
    }).catch(function(error){
        console.log(error);
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
                alert("Failed to get information for upload: " + textStatus + " " + error +". Send me this message on Twitter so I can fix this bug.");
                reject(error);
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
            error: function (xhr, textStatus, error) {
                console.log("Failed to get information for upload: " + textStatus + " " + error +". Send this error message to me on Twitter so I can fix this bug.");

                //TODO: This is temporary since we don't expect the redirect to do anything.
                resolve()
                //reject(error);
            },
            success: function () {
                resolve()
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
        type: "post",
        url: "/discussion-audio-uploaded",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            console.error(error);
            alert("Failed to create new discussion: " + textStatus + " " + error + ". Send me a message via Twitter so I can fix this bug.");
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
    sendRequestForCDNInfo().then((cdnInfo) => {
        return uploadFormToCDN(cdnInfo);
    }).then(() => {
        sendReplyCreationRequest(stateMachine); //TODO: Should this be a promise?
    }).catch(function (error) {
        console.log(error);
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
            alert("Failed to reply to discussion. " + textStatus + " " + error + ". Send me a message via Twitter so I can fix this bug.");
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
        globalStateMachine.complete();
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
        alert("You're browser doesn't support microphone access.\r\nTry the latest version of Chrome/Firefox or upgrading your OS.\r\n(error: microphone support)");
        window.location.href = "/";
        return;
    }
    if (doestNotExist(MediaRecorder)) {
        alert("You're browser doesn't support microphone access.\r\nTry the the latest version of Chrome/Firefox or upgrading your OS.\r\n(error: media support)");
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