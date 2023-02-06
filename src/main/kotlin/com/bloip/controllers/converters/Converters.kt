package com.bloip.controllers.converters

import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.user.User.UserId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/**
 * Created by Usman Mutawakil on 1/13/23.
 */

@Component
class Converters {
    @Component
    class UserIdConverterLong : Converter<Long, UserId> {
        override fun convert(source: Long): UserId? {
            return UserId(source)
        }
    }
    @Component
    class UserIdConverterString : Converter<String, UserId> {
        override fun convert(source: String): UserId? {
            return UserId(source.toLong())
        }
    }

    @Component
    class DiscussionIdConverterLong : Converter<Long, DiscussionId> {
        override fun convert(source: Long): DiscussionId? {
            return DiscussionId(source)
        }
    }

    @Component
    class DiscussionIdConverterString : Converter<String, DiscussionId> {
        override fun convert(source: String): DiscussionId? {
            return DiscussionId(source.toLong())
        }
    }
}