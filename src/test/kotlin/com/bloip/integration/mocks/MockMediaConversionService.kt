package com.bloip.integration.mocks

import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.MediaConversionService

/**
 * Created by Usman Mutawakil on 10/23/22.
 */
class MockMediaConversionService : MediaConversionService() {
    public var count = 0
    public var ran   = false

    override fun convert(discussion: Discussion?, comment: Comment, discussionService: DiscussionService, commentService: CommentService) {
        ran = true
        count++
    }
}