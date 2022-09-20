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
    var formData = new FormData();
    formData.append("title", $("#discussion-title").val());
    formData.append("topicId", $("#discussion-topic").val());

    $.ajax({
        type: "post",
        url: "/new-discussion/create",
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
        sendReply(stateMachine);
    };

    this.hide = function() {
        $("#replying-state-view").css("display", "none");
    };
})();

function sendReply(stateMachine) {
    var formData = new FormData();
    var discussionId = parseInt($("#reply-discussion-id").text());

    formData.append("discussionId", discussionId);

    $.ajax({
        type: "post",
        url: "/reply",
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
        explainWebPushNotificationsIfNeeded();
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
        explainWebPushNotificationsIfNeeded();
    };
    this.hide = function() {};
})();


/** Web push notifications **/
function explainWebPushNotificationsIfNeeded() {
    if (!('Notification' in window)) {
        alert("Your current browser doesn't support web notification.\r\n" +
            " In the future if you want to be notified when someone responds then try a desktop computer with Google chrome or FireFox.");
        return;
    }
    if(Notification.permission === "denied" ) { //TODO: User has already subscribed for notifications. Should we refresh their key?
        alert("You've blocked web notifications from this site so you won't be directly notified in your browser when someone responds.\r\n" +
            " If you change your mind and wish to allow notifications then you will need to manually change your settings. \r\n" +
            " You can send us a message on twitter @BloipApp if you need help on how to do that.");
        return;
    }
    if(Notification.permission === "granted" && (localStorage.getItem("webpush") !== null)) { //TODO: User has already subscribed for notifications. Should we refresh their key?
        console.log("User already requested and uploaded web push subscription info. Skipping this step.");
        return;
    }

    /** New users below **/

    var message = "We want to notify you in your browser when someone has responded to your post after you have left the site.\r\n" +
        "We don't use email or phone numbers so this is the only way to reach you directly. Please allow web notifications on the next screen. \r\n" +
        "Your identity will continue to remain anonymous but you can also follow us on twitter @BloipApp";
    if(confirm(message)) {
        subscribeToWebPushNotifications();
    }
}
