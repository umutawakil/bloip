package com.bloip.utilities

import com.bloip.configuration.ApplicationProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Component
class DiscussionUtility (@Autowired val applicationProperties : ApplicationProperties) {
    fun getDiscussionUrlFromId(discussionId: Long) : String {
        return applicationProperties.baseURL + "/b/" + discussionId
    }
}