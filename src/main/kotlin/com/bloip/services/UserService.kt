package com.bloip.services

import com.bloip.caches.UserCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.HttpSessionConfig
import com.bloip.domain.EmailAddress
import com.bloip.domain.User
import com.bloip.domain.authentication.AuthenticationUserDetail
import com.bloip.domain.authentication.Role
import com.bloip.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
            val rootUser: User = createNewUser()
            rootUser.authenticationUserDetail = AuthenticationUserDetail(
                user     = rootUser,
                email    = EmailAddress(applicationProperties.shogunUsername),
                password = passwordEncoder.encode(applicationProperties.shogunPassword)
            )
            /** Give it every role **/
            for(r: Role in roleService.getRoles()) {
                rootUser.authenticationUserDetail!!.roles.add(r)
            }
            save(user = rootUser)
            println("RootUser: " + rootUser.id)
        }
    }

    fun createNewUser() : User {
        val user: User = userRepository.save(User())
        userCache.add(user)
        return user
    }

    fun findById(userId: Long) : User? {
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
}