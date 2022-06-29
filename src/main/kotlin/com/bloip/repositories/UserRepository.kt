package com.bloip.repositories

import com.bloip.domain.User
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
interface UserRepository : PagingAndSortingRepository<User, Long> {

}