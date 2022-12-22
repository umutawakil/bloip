
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
    createDiscussionFsm.next();
};