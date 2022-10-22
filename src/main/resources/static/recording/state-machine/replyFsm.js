import {
    microphonePermission,
    idleState,
    recordingState,
    recordingCompleteState,
    replyingState,
    replyConfirmationState

} from "./shared/states.js";

var replyDiscussionFsm = new Bloip.StateMachine();
globalStateMachine = replyDiscussionFsm;
replyDiscussionFsm.addStates(
    [
        microphonePermission,
            idleState,
            recordingState,
            recordingCompleteState,
            replyingState,
            replyConfirmationState
    ]
);
/** Start the state machine **/
replyDiscussionFsm.next();