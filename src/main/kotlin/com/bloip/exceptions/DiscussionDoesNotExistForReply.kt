package com.bloip.exceptions

import com.bloip.domain.discussion.Discussion

/**
 * Created by Usman Mutawakil on 1/14/23.
 */
class DiscussionDoesNotExistForReply: RuntimeException {
    constructor(discussionId: Discussion.DiscussionId) {
        RuntimeException("The discussion to be replied to no longer exists. ${discussionId}")
    }
}