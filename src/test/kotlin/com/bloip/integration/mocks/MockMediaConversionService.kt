package com.bloip.integration.mocks

import com.bloip.domain.discussion.Discussion
import com.bloip.services.audioconversion.AudioConversionRequestService

/**
 * Created by Usman Mutawakil on 10/23/22.
 */
class MockMediaConversionService : AudioConversionRequestService {
    public var count = 0
    public var ran   = false

    override fun startConvertingAudioFile(discussion: Discussion, trackNumber: Int) {
        ran = true
        count++
    }
}