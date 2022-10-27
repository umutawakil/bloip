package com.bloip.utilities

import com.bloip.msc.Constants
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 10/26/22.
 */
class DiscussionUtilityTest {
    @Test
    fun canDetectUnwantedFileTypes() {
        val unwantedFiles: List<String> = listOf("test.mp3","test.webm")

        unwantedFiles.forEach {
            assertTrue(DiscussionUtility.fileNeedsToBeConverted(fileName = it))
        }
    }
    @Test
    fun canDetectWantedFileType() {
        assertFalse(DiscussionUtility.fileNeedsToBeConverted(fileName = "test.mp4"))
    }

    @Test
    fun CAN__RETURN__UNCONVERTED__FILE__IF__STILL__NEEDS__CONVERSION() {
        val fileName = "recording.webm"
        val url = DiscussionUtility.getPotentiallyConvertedFileLocation(needsConversion = true, fileName = fileName)
        assertTrue(url.endsWith(".webm"))
    }

    @Test
    fun CAN__RETURN__CONVERTED__FILE__OR__ORIGINAL__IF__FILE__DOESNT__NEED__CONVERSION() {
        val fileName = "recording." + Constants.Target_Audio_File_Extension
        val url = DiscussionUtility.getPotentiallyConvertedFileLocation(needsConversion = true, fileName = fileName)
        assertTrue(url.endsWith(Constants.Target_Audio_File_Extension))
    }
}