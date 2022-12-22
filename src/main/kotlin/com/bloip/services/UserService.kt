package com.bloip.services

import com.bloip.caches.UserCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.HttpSessionConfig
import com.bloip.domain.value.EmailAddress
import com.bloip.domain.user.User
import com.bloip.domain.user.authentication.AuthenticationUserDetail
import com.bloip.domain.user.authentication.Role
import com.bloip.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserService (
    @Autowired private val userRepository: UserRepository,
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val roleService: RoleService,
    @Autowired private val userCache: UserCache,
    @Autowired private val passwordEncoder: PasswordEncoder,
    @Autowired private val userCookieService: UserCookieService,
    @Autowired private val httpSessionConfig: HttpSessionConfig
) : UserDetailsService
{
    @Transactional
    @PostConstruct
    fun init() {
        /** Create the root user if it doesn't exist **/
        if(!usernameExists(applicationProperties.shogunUsername)) {
            createAShogun(
                username = applicationProperties.shogunUsername,
                password = applicationProperties.shogunPassword
            )
        }
    }

    fun createAShogun(username: String, password: String) : User {
        val rootUser: User = createNewUser()
        rootUser.authenticationUserDetail = AuthenticationUserDetail(
            user     = rootUser,
            email    = EmailAddress(username),
            password = passwordEncoder.encode(password)
        )
        /** Give it every role **/
        for(r: Role in roleService.getRoles()) {
            rootUser.authenticationUserDetail!!.roles.add(r)
        }
        return save(user = rootUser)
    }

    fun createNormalUser(username: String, password: String) : User {
        val rootUser: User = createNewUser()
        rootUser.authenticationUserDetail = AuthenticationUserDetail(
            user     = rootUser,
            email    = EmailAddress(username),
            password = passwordEncoder.encode(password)
        )
        return save(user = rootUser)
    }

    fun createNewUser() : User {
        val user: User = userRepository.save(User())
        userCache.add(user)
        return user
    }

    fun findById(userId: Long?) : User? {
        if(userId == null) {
            return null
        }
        return userCache.findById(userId)
    }

    fun usernameExists(username: String) : Boolean {
        return userCache.usernameExists(username = username)
    }

    fun findByUsername(username: String) : User? {
        return userCache.findByUserName(username = username)
    }

    fun isNotActiveUser(userId: Long) : Boolean {
        return !isActiveUser(userId)
    }

    fun isActiveUser(userId: Long) : Boolean {
        return userCache.contains(userId)
    }

    fun findByCookieCode(code: String) : User? {
        return userCookieService.findByCode(code)?.getUser()
    }

    @Transactional
    fun resetCookies(user: User, code: String, ipAddress: String) {
        deleteCookies(
            userId = user.id
        )
        saveCookieInfo(
            user = user,
            code = code,
            ipAddress = ipAddress
        )
    }

    fun saveCookieInfo(user: User, code: String, ipAddress: String) {
        userCookieService.saveCookieInfo(user, code, ipAddress)
    }

    private fun deleteCookies(userId: Long) {
        userCookieService.deleteCookies(
            userId = userId
        )
    }

    fun numOfUsersOnline() : Int {
        return httpSessionConfig.numOfSessions()
    }

    fun save(user: User) : User {
        val updatedUser: User = userRepository.save(user)
        userCache.add(updatedUser)
        return updatedUser
    }

    /** This needs to be used instead of save if you are trying to update an email otherwise the old cache index with the old email will be laying around **/
    fun updateEmail(user: User, newEmail: EmailAddress) {
        val oldEmail = user.getEmail()!!
        user.authenticationUserDetail!!.setEmailAddress(newEmail)
        val updatedUser: User = userRepository.save(user)
        userCache.purgeEmail(oldEmail)
        userCache.add(updatedUser)
    }

    fun delete(userId: Long) {
        userRepository.deleteById(userId)
        val user: User = findById(userId = userId) ?: return
        userCache.delete(user = user)
    }

    override fun loadUserByUsername(username: String): AuthenticationUserDetail {
        return userCache.loadByUserName(username = username) ?: throw UsernameNotFoundException("Username not found")
    }

    fun updateDiscussionLimitStats(user: User) : User {
        if(user.firstDiscussionCreationInLastDay == null) {
            user.firstDiscussionCreationInLastDay = Date()
            user.discussionCreationCount = 0
        }
        user.discussionCreationCount++
        return save(user)
    }

    fun isDiscussionCreationLimitReached(user: User) : Boolean {
        if (shouldResetCreationWindow(user)) {
            return false
        }
        if (user.discussionCreationCount < applicationProperties.maxDiscussionCreationsPerDay) {
            return false
        }
        return true
    }

    fun shouldResetCreationWindow(user: User) : Boolean {
        if (user.firstDiscussionCreationInLastDay == null ) {
            return false
        }
        val DAY_IN_MILLIS = 3600*24*1000L
        val finalDate     = Date(user.firstDiscussionCreationInLastDay!!.time + DAY_IN_MILLIS)
        if (Date().after(finalDate)) {
            return true
        }
        return false
    }

    fun resetDiscussionCreationWindow(user: User) : User {
        user.resetDiscussionCreationWindow()
        return save(user)
    }

    fun deleteAll() {
        userCache.deleteAll()
        for (u in userRepository.findAll()) {
            if (u.getEmail() != applicationProperties.shogunUsername) {
                userRepository.delete(u)
            }
        }
    }
}