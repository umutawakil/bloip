package com.bloip.repositories

import com.bloip.domain.Discussion
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
interface DiscussionRepository : PagingAndSortingRepository<Discussion, Int> {
}