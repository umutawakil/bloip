package com.bloip.repositories

import com.bloip.domain.Comment
import com.bloip.domain.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 6/30/22.
 */
interface CommentRepository : CrudRepository <Comment, Long> {
}