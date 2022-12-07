package com.bloip.repositories

import com.bloip.domain.authentication.AuthenticationUserDetail

import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
interface UserDetailRepository: CrudRepository<AuthenticationUserDetail, Long> {

}