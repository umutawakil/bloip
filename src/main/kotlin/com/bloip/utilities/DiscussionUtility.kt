package com.bloip.utilities

import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.EnvironmentConfigs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Component
class DiscussionUtility (@Autowired val applicationProperties : ApplicationProperties) {
    companion object {

        /** At the time of writing this non mp4 files are converted and stored in a sub folder **/
        fun getPotentiallyConvertedFileLocation(fileName: String) : String {
            return if (fileName.endsWith("mp4")) {
                EnvironmentConfigs.audioCdnRootUrl + "/" + fileName
            } else {
                EnvironmentConfigs.audioCdnRootUrl + "/output/" + fileName.substring(0,fileName.indexOf("."))+".mp4"
            }
        }
    }

    fun getDiscussionUrlFromId(discussionId: Long) : String {
        return applicationProperties.baseUrl + "/d/" + discussionId
    }
}