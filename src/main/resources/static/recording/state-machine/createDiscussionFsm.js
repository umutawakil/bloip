import {
    discussionTopic,
    discussionInfo,
    discussionVideo,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState

} from "./shared/states.js";

var createDiscussionFsm = new Bloip.StateMachine();
createDiscussionFsm.addStates([
    discussionTopic,
    discussionInfo,
    discussionVideo,
    idleState,
    recordingState,
    recordingCompleteState,
    creatingState,
    discussionConfirmationState
]);

/** Start the state machine **/
createDiscussionFsm.next();