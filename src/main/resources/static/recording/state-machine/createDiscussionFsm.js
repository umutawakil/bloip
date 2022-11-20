import {
    microphonePermission,
    discussionTopic, //TODO: Not in use for now.
    discussionInfo,
    discussionVideo,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState

} from "./shared/states.js";

var createDiscussionFsm = new Bloip.StateMachine();
globalStateMachine = createDiscussionFsm;
createDiscussionFsm.addStates([
    discussionInfo,
    microphonePermission,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState
]);

/** Start the state machine **/
createDiscussionFsm.next();