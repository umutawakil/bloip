package com.bloip.msc

/**
 * Created by Usman Mutawakil on 10/23/22.
 */
class Constants {
    companion object {
        const val Target_Audio_File_Extension       = "mp4"
        const val Target_Audio_File_Content_Type    = "audio/mp4"
        const val Temporary_Audio_File_Extension    = "webm"
        const val Temporary_Audio_File_Content_Type = "audio/webm; codecs=opus"
        const val REMOTE_SERVICES_ON                = "YES" //This is not an environment variable, just used to reduce duplication through an abstraction instead of typing 'YES' in multiple places
    }
}