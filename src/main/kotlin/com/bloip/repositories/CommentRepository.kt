package com.bloip.repositories

import com.bloip.domain.Comment
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 6/30/22.
 */
interface CommentRepository : CrudRepository <Comment, Long> {
}