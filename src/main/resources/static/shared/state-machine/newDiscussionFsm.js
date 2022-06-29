import { WASM } from "../../shared/wasmRecorder.js";

var newDiscussionFsm = new Bloip.StateMachine();

/** Discussion Info State**/
var discussionInfo = new (function() {
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
                checkIfUnique(stateMachine);
            });
            $("#discussion-title").keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if(keycode === 13) {
                    checkIfUnique(stateMachine);
                }
            })
        });
    };

    var checkIfUnique = function (stateMachine) {
        if($("#discussion-title").val() == "" || $("#discussion-title").val().length < 3) {
            alert("Your title is too short. Please make it at least 3 characters");
            return;
        }

        $.ajax({
            type: "get",
            url: "/new-discussion/is-unique?title=" + encodeURIComponent($("#discussion-title").val()),
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
    }

    this.show = function() {
        $("#discussion-info-state-view").css("display", "block");
    };

    this.run = function(stateMachine) {};

    this.hide = function() {
        $("#discussion-info-state-view").css("display", "none");
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
newDiscussionFsm.addState(recordingState);

/** Recording Complete State**/
var recordingCompleteState = new (function() {
    this.getName = function() {
        return "recording-complete";
    };

    this.initEvents = function(stateMachine) {
        $(document).ready(function() {
            $("#save-button").click(function() {
                stateMachine.next();
            });
            $("#delete-button").click(function() {
                stateMachine.reset();
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
newDiscussionFsm.addState(recordingCompleteState);

//TODO: Create the discussion and/or put it in a eventually consistent state/flag and flip it when all is well.

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
    var formData = new FormData();
    formData.append("title", $("#discussion-title").val());

    $.ajax({
        type: "post",
        url: "/new-discussion/create",
        contentType: false,
        processData: false,
        data: formData,
        error: function (xhr, textStatus, error) {
            alert("Failed to create new discussion" + textStatus + " " + error);
        },
        success: function (url) {
            stateMachine.next(url);
        }
    });
}
newDiscussionFsm.addState(creatingState);

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

    this.run = function() {};
    this.hide = function() {};

})();
newDiscussionFsm.addState(discussionConfirmationState);

/** Start the state machine **/
newDiscussionFsm.next();