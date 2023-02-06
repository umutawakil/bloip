package com.bloip.utilities

import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.EnvironmentConfigs
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Component
class DiscussionUtility (@Autowired val applicationProperties : ApplicationProperties) {
    companion object {

        /** At the time of writing this non mp4 files are converted and stored in a sub folder **/
        fun getPotentiallyConvertedFileLocation(needsConversion: Boolean, fileName: String) : String {
            return if (needsConversion || fileName.endsWith(Constants.Target_Audio_File_Extension)) {
                EnvironmentConfigs.audioCdnRootUrl + "/" + fileName
            } else {
                EnvironmentConfigs.audioCdnRootUrl + "/output/" +
                        fileName.substring(0,fileName.indexOf("."))+"." + Constants.Target_Audio_File_Extension
            }
        }

        fun fileNeedsToBeConverted(fileName: String) : Boolean{
            return !fileName.endsWith(Constants.Target_Audio_File_Extension)
        }
    }

    fun getDiscussionUrlFromId(discussionId: Discussion.DiscussionId) : String {
        return applicationProperties.baseUrl + "/d/" + discussionId
    }
}