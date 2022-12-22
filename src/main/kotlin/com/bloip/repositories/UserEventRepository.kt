package com.bloip.repositories

import com.bloip.domain.UserEvent
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 12/18/22.
 */
interface UserEventRepository : CrudRepository<UserEvent, Long> {
}