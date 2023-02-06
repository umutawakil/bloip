package com.bloip.services.admin

import com.bloip.domain.discussion.Discussion

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 11/21/22.
 */
@Service
class ModerationService(
    @Autowired private val adminService: AdminService,
    
)  {
    fun moderateTitle(discussion: Discussion) {
        Discussion.censureTitle(discussion)
        adminService.recordEvent("Discussion title removed for discussion: ${discussion.id}")
    }

    fun moderateComment(discussion: Discussion, trackNumber: Int){
        Discussion.censureComment(discussion = discussion, trackNumber = trackNumber)
        adminService.recordEvent(
            "Comment hidden for" +
                    " discussion: ${discussion.id}," +
                    " Track: ${trackNumber}"
        )
    }

    fun moderateUser(discussion: Discussion, trackNumber: Int){
        Discussion.censureUser(discussion = discussion, trackNumber = trackNumber)
        adminService.recordEvent(
            "User moderated for" +
                    " discussion: ${discussion.id}," +
                    " Track: ${trackNumber}"
        )
    }
}