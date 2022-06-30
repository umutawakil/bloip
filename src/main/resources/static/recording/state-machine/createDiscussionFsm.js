import {
    discussionInfo,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState

} from "./shared/states.js";

var createDiscussionFsm = new Bloip.StateMachine();
createDiscussionFsm.addStates([
    discussionInfo,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState
]);

/** Start the state machine **/
createDiscussionFsm.next();