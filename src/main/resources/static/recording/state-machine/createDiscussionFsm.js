
var createDiscussionFsm = new Bloip.StateMachine();
globalStateMachine = createDiscussionFsm;
createDiscussionFsm.addStates([
    limitChecks,
    discussionInfo,
    microphonePermission,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState
]);

window.onload = function() {
    /** Start the state machine **/
    try {
        createDiscussionFsm.next();
    } catch (e) {
        logUserEvent("Error-recording-createDiscussionFsm.next", e.message);
        alert($("#t-unexpected-error").text());
        window.location.href = "/";
    }
};