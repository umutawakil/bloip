
var replyDiscussionFsm = new Bloip.StateMachine();
globalStateMachine = replyDiscussionFsm;
replyDiscussionFsm.addStates(
    [
        limitChecks,
        microphonePermission,
            idleState,
            recordingState,
            recordingCompleteState,
            replyingState,
            replyConfirmationState
    ]
);

window.onload = function() {
    /** Start the state machine **/
    try {
        replyDiscussionFsm.next();
    } catch (e) {
        logUserEvent("Error-recording-replyDiscussionFsm.next", e.message);
        alert($("#t-unexpected-error").text());
        window.location.href = "/";
    }
};