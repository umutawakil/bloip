import { WASM } from "../../shared/wasmRecorder.js";
//import { WASM } from "../vmsg.js";

var newDiscussionFsm = new Bloip.StateMachine();

/** Discussion Info State**/
var discussionInfo = new (function() {
    this.getName = function() {
        return "discussion-info";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#nd-discussion-info-submit-button").click(function() {
                if($("#nd-discussion-title").val() == "" || $("#nd-discussion-title").val().length < 3) {
                    alert("Your title is too short. Please make it at least 3 characters");
                    return;
                }

                $.ajax({
                    type: "get",
                    url: "/new-discussion/is-unique?title=" + encodeURIComponent($("#nd-discussion-title").val()),
                    contentType: false,
                    processData: false,
                    error: function (xhr, textStatus, error) {
                        alert("Failed to validate discussion title. " + textStatus + " " + error);
                    },
                    success: function (data) {
                        if(data == -1) {
                            alert("That title is already taken. Try something else");
                        } else {
                            stateMachine.next();
                        }
                    }
                });
            });
        });
    };

    this.show = function() {
        $("#nd-discussion-info-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {

    };

    this.hide = function() {
        $("#nd-discussion-info-state-view").css("display", "none");
    };
})();
newDiscussionFsm.addState(discussionInfo);

/** Idle State**/
var idleState = new (function() {
    this.getName = function() {
        return "idle";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#nd-record-button").click(function() {
                stateMachine.next();
            });
        });
    };

    this.show = function() {
        $("#nd-idle-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {

    };

    this.hide = function() {
        $("#nd-idle-state-view").css("display", "none");
    };
})();
newDiscussionFsm.addState(idleState);

/** Recording State **/
var recordingState = new (function() {
    var MAX_COUNT = 6;
    var counter = MAX_COUNT;
    var interval;

    this.getName = function() {
        return "recording";
    };

    this.initEvents = function (stateMachine) {
        $(document).ready(function () {
            $("#nd-stop-button").click(function () {
                stateMachine.next();
            });
        });
    };

    this.show = function (stateMachine) {
        $("#nd-recording-state-view").css("display", "block");
        $("#nd-counter-display").text(counter);

        interval = setInterval(function () {
                counter--;
                $("#nd-counter-display").text(counter);
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
        $("#nd-recording-state-view").css("display", "none");
        clearInterval(interval);
        counter = MAX_COUNT;

        WASM.stopRecording();
    };
})();
newDiscussionFsm.addState(recordingState);

/** Recording Complete State**/
var recordingCompleteState = new (function() {
    this.getName = function() {
        return "recording-complete";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#nd-save-button").click(function() {
                stateMachine.next();
            });
            $("#nd-delete-button").click(function() {
                stateMachine.reset();
            });
        });
    };

    this.show = function() {
        $("#nd-recording-complete-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {

    };

    this.hide = function() {
        $("#nd-recording-complete-state-view").css("display", "none");
    };
})();
newDiscussionFsm.addState(recordingCompleteState);

//TODO: Create the discussion and/or put it in a eventually consistent state/flag and flip it when all is well.

/** Saving State**/
var savingState = new (function() {
    this.getName = function() {
        return "saving";
    };

    this.initEvents = function(stateMachine) {
    };

    this.show = function() {
        $("#nd-saving-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {
        $("#nd-saving-display").text("Uploading");
        setTimeout(function() {
            stateMachine.next();
        }, 5000);
    };

    this.hide = function() {
        $("#nd-save-state-view").css("display", "none");
    };
})();
newDiscussionFsm.addState(savingState);

/** Confirmation State**/
var confirmationState = new (function() {
    this.getName = function() {
        return "confirmation";
    };

    this.initEvents = function(stateMachine) {
    };

    this.show = function() {
        $("#nd-confirmation-state-view").css("display", "block");
    };

    this.run = function() {
        $("#nd-confirmation-display").text("Your new discussion has been created");
    };

    this.hide = function() {
        $("#nd-confirmation-state-view").css("display", "none");
    };

})();
newDiscussionFsm.addState(confirmationState);

/** Start the state machine **/
newDiscussionFsm.next();