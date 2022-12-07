package com.bloip.repositories

import com.bloip.domain.Token
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 12/5/22.
 */
interface TokenRepository : CrudRepository<Token, Long> {
}