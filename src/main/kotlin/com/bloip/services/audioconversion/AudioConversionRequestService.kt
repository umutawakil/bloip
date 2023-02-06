package com.bloip.services.audioconversion

import com.bloip.domain.discussion.Discussion

/**
 * This was created solely for unit testing, although that code could
 * be replaced with Mockito logic (I was in a hurry...); however, it seems
 * the mediaconvert service is expensive so it is possible more than one implementation
 * will be created.
 */
interface AudioConversionRequestService {
    fun startConvertingAudioFile(discussionId: Discussion.DiscussionId, fileName: String, trackNumber: Int)
}