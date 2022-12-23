package com.bloip.services.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 11/21/22.
 */
@Service
class ModerationService(
    @Autowired private val adminService: AdminService,
    @Autowired private val discussionService: DiscussionService
)  {
    fun moderateTitle(discussion: Discussion) {
        discussionService.censureTitle(discussion)
        adminService.recordEvent("Discussion title removed for discussion: ${discussion.id}")
    }

    fun moderateComment(discussion: Discussion, trackNumber: Int){
        discussionService.censureComment(discussion = discussion, trackNumber = trackNumber)
        adminService.recordEvent(
            "Comment hidden for" +
                    " discussion: ${discussion.id}," +
                    " Track: ${trackNumber}"
        )
    }

    fun moderateUser(discussion: Discussion, trackNumber: Int){
        discussionService.censureUser(discussion = discussion, trackNumber = trackNumber)
        adminService.recordEvent(
            "User moderated for" +
                    " discussion: ${discussion.id}," +
                    " Track: ${trackNumber}"
        )
    }
}