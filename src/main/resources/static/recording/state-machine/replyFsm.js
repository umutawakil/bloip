
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
    replyDiscussionFsm.next();
};