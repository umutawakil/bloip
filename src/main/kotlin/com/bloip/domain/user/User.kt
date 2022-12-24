package com.bloip.domain.user

import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.HttpSessionConfig
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.user.authentication.AuthenticationUserDetail
import com.bloip.domain.user.authentication.Role
import com.bloip.domain.value.EmailAddress
import com.bloip.repositories.UserRepository
import com.bloip.services.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.ui.set
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
class User : StandardDomainObject
{
    /** This component is only used for authentication and authorization via Spring Security **/
    @Service
    class UserService : UserDetailsService
    {
        override fun loadUserByUsername(username: String): AuthenticationUserDetail {
            return User.loadUserByUsername(username = username)
        }
    }

    @Component
    class SpringAdapter(
        @Autowired private val userRepository: UserRepository,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val roleService: RoleService,
        @Autowired private val passwordEncoder: PasswordEncoder,
        @Autowired private val userCookieService: UserCookieService,
        @Autowired private val httpSessionConfig: HttpSessionConfig,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val emailService: EmailService
    ) {
        @PostConstruct
        fun init() {
            User.userRepository        = userRepository
            User.applicationProperties = applicationProperties
            User.roleService           = roleService
            User.passwordEncoder       = passwordEncoder
            User.userCookieService     = userCookieService
            User.httpSessionConfig     = httpSessionConfig
            User.emailService          = emailService


            /** Initial set of users for prepopulating various caches **/
            val initialUsers = User.userRepository.findAll()

            /** Initialize the user cache **/
            loggingService.log("Initializing user cache")
            userCache = UserCache(
                applicationProperties = applicationProperties,
                initialUsers          = initialUsers
            )
            loggingService.log("User cache initialized\r\n\r\n")

            /** Initialize the tokens **/
            for(u in initialUsers) {
                for (t in u.tokens) {
                    tokensByToken[t.value] = t
                }
            }

            /** Create the root user if it doesn't exist **/
            if(!usernameExists(Companion.applicationProperties.shogunUsername)) {
                createAShogun(
                    username = Companion.applicationProperties.shogunUsername,
                    password = Companion.applicationProperties.shogunPassword
                )
            }
        }
    }

    private class UserCache {
        private val users: MutableMap<Long, User> = ConcurrentHashMap<Long, User>()
        private val authenticationDetailsByUserName: MutableMap<String, AuthenticationUserDetail> = ConcurrentHashMap()
        private val applicationProperties: ApplicationProperties

        constructor(applicationProperties: ApplicationProperties, initialUsers: Iterable<User>) {
            this.applicationProperties = applicationProperties
            for(u: User in initialUsers) {
                users[u.id] = u
                if (u.authenticationUserDetail != null) {
                    authenticationDetailsByUserName[u.authenticationUserDetail!!.username] = u.authenticationUserDetail!!
                }
            }
        }

        fun findByIdFromCache(userId: Long) : User? {
            return users[userId]
        }

        fun findByUserNameFromCache(username: String) : User? {
            return authenticationDetailsByUserName[username]?.user
        }

        fun loadByUserNameFromCache(username: String) : AuthenticationUserDetail? {
            return authenticationDetailsByUserName[username]?.user?.authenticationUserDetail
        }

        fun usernameExistsInCache(username: String) : Boolean {
            return authenticationDetailsByUserName[username] != null
        }

        fun addToCache(user: User) {
            users[user.id] = user
            if (user.authenticationUserDetail != null) {
                authenticationDetailsByUserName[user.authenticationUserDetail!!.username] = user.authenticationUserDetail!!
            }
        }

        fun deleteFromCache(user: User) {
            users.remove(user.id)
            if (user.authenticationUserDetail != null) {
                authenticationDetailsByUserName.remove(user.authenticationUserDetail!!.username)
            }
        }

        fun purgeEmail(email: String) {
            authenticationDetailsByUserName.remove(email)
        }

        fun deleteAllFromCache() {
            if(applicationProperties.enableRemoteServices != "NO" && applicationProperties.environment != "dev") {
                throw RuntimeException("Delete all not allowed in environment")
            }
            for (u in users.values) {
                if(u.getEmail() != applicationProperties.shogunUsername) {
                    users.remove(key = u.id)
                    if(u.authenticationUserDetail != null && authenticationDetailsByUserName.contains(u.getEmail())) {
                        authenticationDetailsByUserName.remove(u.getEmail())
                    }
                }
            }
            // users.clear()
            // authenticationDetailsByUserName.clear()
        }
    }
    companion object {
        private lateinit var userRepository: UserRepository
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var roleService: RoleService
        private lateinit var userCache: UserCache
        private lateinit var passwordEncoder: PasswordEncoder
        private lateinit var userCookieService: UserCookieService
        private lateinit var httpSessionConfig: HttpSessionConfig
        private lateinit var emailService: EmailService

        /** Non Spring dependencies **/
        private val tokensByToken: MutableMap<String, Token> = ConcurrentHashMap()

        /** ***/

        fun createAShogun(username: String, password: String): User {
            val rootUser: User = createNewUser()
            rootUser.authenticationUserDetail = AuthenticationUserDetail(
                user = rootUser,
                email = EmailAddress(username),
                password = passwordEncoder.encode(password)
            )
            /** Give it every role **/
            for (r: Role in roleService.getRoles()) {
                rootUser.addRole(r)
            }
            return rootUser.addAuthenticationDetails(
                email = username,
                password = password
            )
        }

        fun createNormalUser(username: String, password: String): User {
            val rootUser: User = createNewUser()
            return rootUser.addAuthenticationDetails(
                email = username,
                password = password
            )
        }

        fun createNewUser(): User {
            val user: User = userRepository.save(User())
            userCache.addToCache(user)
            return user
        }

        fun findById(userId: Long?): User? {
            if (userId == null) {
                return null
            }
            return userCache.findByIdFromCache(userId)
        }

        fun usernameExists(username: String): Boolean {
            return userCache.usernameExistsInCache(username = username)
        }

        fun findByUsername(username: String): User? {
            return userCache.findByUserNameFromCache(username = username)
        }

        fun findByEmail(email: String): User? {
            return findByUsername(username = email)
        }

        fun findByCookieCode(code: String): User? {
            return userCookieService.findByCode(code)?.getUser()
        }

        fun loadUserByUsername(username: String): AuthenticationUserDetail {
            return userCache.loadByUserNameFromCache(username = username)
                ?: throw UsernameNotFoundException("Username not found")
        }

        fun deleteAll() {
            userCache.deleteAllFromCache()
            for (u in userRepository.findAll()) {
                if (u.getEmail() != applicationProperties.shogunUsername) {
                    userRepository.delete(u)
                }
            }
        }

        fun findByToken(tokenValue: String?): User? {
            if(tokenValue == null) return null
            return tokensByToken[tokenValue]?.user
        }

        fun changeEmailFromToken(tokenValue: String, success: ()-> Any, failure: () -> Any) : Any {
            val token: Token = tokensByToken[tokenValue] ?: return failure.invoke()
            token.user.updateEmail(newEmail = EmailAddress(token.email))
            token.user.removeToken(tokenValue = tokenValue)
            return success.invoke()
        }

        fun completeSignupFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
            val token: Token = tokensByToken[tokenValue] ?: return failure.invoke()
            val user = token.user

            user.addAuthenticationDetails(
                email = token.email,
                password = passwordEncoder.encode(password)
            )
            user.removeToken(tokenValue = tokenValue)
            return success.invoke()
        }

        fun changePasswordFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
            val token: Token = tokensByToken[tokenValue] ?: return failure.invoke()
            val user = token.user

            user.changePassword(password = password)
            user.removeToken(tokenValue = tokenValue)
            return success.invoke()
        }
    }
    /**** End of static methods ************************************************************/

    /**** User instance fields and methods below *******************************************/

    private var censured:      Boolean = false
    private var censureDate:   Date?   = null
    private var emailDisabled: Boolean = false

    /** Don't cascade delete. The DB is using cascade on delete for its foreign keys **/
    @OneToOne(
        optional = true,
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.MERGE,
            CascadeType.DETACH
        ]
    )
    @JoinColumn(name = "user_detail_id", referencedColumnName = "user_id", nullable = true)
    private var authenticationUserDetail: AuthenticationUserDetail? = null

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.MERGE,
            CascadeType.DETACH
        ],
        mappedBy = "user"
    )
    private val tokens: MutableSet<Token> = mutableSetOf()
    @Entity
    @Table(name = "token")
    private class Token : StandardDomainObject {
        @JoinColumn(name="user_id", referencedColumnName = "id")
        @ManyToOne(fetch = FetchType.EAGER, optional = false)
        val user: User
        @Column
        val email: String
        @Column
        val value: String
        @Column
        val creationTimestamp: Date

        constructor(user: User, email: String) {
            this.user              = user
            this.email             = email
            this.value             = UUID.randomUUID().toString()
            this.creationTimestamp = Date()
        }
    }

    private fun addNewToken(email: String) : Token {
        val token = Token(
            user = this,
            email = email
        )
        this.tokens.add(token)
        this.save()

        tokensByToken[token.value] = token

        return token
    }

    fun removeToken(tokenValue: String) {
        val token: Token = tokensByToken[tokenValue] ?: return
        this.tokens.remove(token)
        tokensByToken.remove(token.value)
        this.save()
    }

    constructor()

    /** Code below is a temporary quick fix for limiting users to 10 discussions a day **/
    @Column
    private var discussionCreationCount: Int = 0

    @Column
    private var firstDiscussionCreationInLastDay: Date? = null

    @Version
    private val version = 0

    @Transient
    fun getEmail() : String? {
        if (this.authenticationUserDetail == null) return null

        return this.authenticationUserDetail!!.username
    }

    private fun addRole(role: Role) {
        if(this.authenticationUserDetail == null) {
            return
        } else {
            this.authenticationUserDetail!!.roles.add(role)
        }
    }

    fun changePassword(password: String) {
        if (this.authenticationUserDetail == null) return
        this.authenticationUserDetail!!.password = passwordEncoder.encode(password)
        this.save()
    }

    fun addAuthenticationDetails(email: String, password: String) : User {
        this.authenticationUserDetail = AuthenticationUserDetail(
            user     = this,
            email    = EmailAddress(email),
            password = passwordEncoder.encode(password)
        )
        return this.save()
    }

    fun resetDiscussionCreationWindow() : User {
        this.discussionCreationCount = 0
        this.firstDiscussionCreationInLastDay = Date()
        return this.save()
    }

    private fun isEmailNotifiable() : Boolean {
        return ((this.authenticationUserDetail != null) && !this.emailDisabled)
    }

    fun updateDiscussionLimitStats() : User {
        if(this.firstDiscussionCreationInLastDay == null) {
            this.firstDiscussionCreationInLastDay = Date()
            this.discussionCreationCount = 0
        }
        this.discussionCreationCount++

        return this.save()
    }

    fun shouldResetCreationWindow() : Boolean {
        if (this.firstDiscussionCreationInLastDay == null ) {
            return false
        }
        val DAY_IN_MILLIS = 3600*24*1000L
        val finalDate     = Date(this.firstDiscussionCreationInLastDay!!.time + DAY_IN_MILLIS)
        if (Date().after(finalDate)) {
            return true
        }
        return false
    }

    private fun isDiscussionCreationLimitReached(maxDiscussionCreationsPerDay: Int) : Boolean {
        if (this.shouldResetCreationWindow()) {
            return false
        }
        if (this.discussionCreationCount < maxDiscussionCreationsPerDay) {
            return false
        }
        return true
    }

    fun showIfDisabled(model: Model) {
        model["disabled"] = emailDisabled
    }

    fun updateNotificationStatus(disabled: Boolean) : User {
        this.emailDisabled = disabled
        return this.save()
    }

    fun censureUser() : User {
        this.censured    = true
        this.censureDate = Date()
        return this.save()
    }

    fun <T> doIfCensured(deny: ()-> T, allow: () -> T) : T {
        return if(this.censured) {
            deny()
        } else {
            allow()
        }
    }

    /** Imported service methods before **************************************/

    private fun save() : User {
        val updatedUser: User = userRepository.save(this)
        userCache.addToCache(updatedUser)
        return updatedUser
    }

    fun resetCookies(code: String, ipAddress: String) {
        deleteCookies()
        saveCookieInfo(
            code = code,
            ipAddress = ipAddress
        )
    }

    private fun saveCookieInfo(code: String, ipAddress: String) {
        userCookieService.saveCookieInfo(this, code, ipAddress)
    }

    private fun deleteCookies() {
        userCookieService.deleteCookies(
            user = this
        )
    }

    /** This needs to be used instead of save if you are trying to update an email otherwise the old cache index with the old email will be lying around **/
    fun updateEmail(newEmail: EmailAddress) {
        val oldEmail = this.getEmail()!!
        this.authenticationUserDetail!!.setEmailAddress(newEmail)
        val updatedUser: User = userRepository.save(this)
        userCache.purgeEmail(oldEmail)
        userCache.addToCache(updatedUser)
    }

    fun sendDiscussionNotificationEmail() {
        if(!this.isEmailNotifiable()) return

        var token:Token        = this.addNewToken(email = this.getEmail()!!)
        var inboxUrl           = applicationProperties.baseUrl + "/inbox"
        var tokenUrl: String   = applicationProperties.baseUrl + "/unsubscribe-email?t=${token.value}"
        var mainMessage        = "<div>Check your inbox to see new messages -> <a href=\"$inboxUrl\"> My Inbox </a></div>"
        var unsubscribeMessage = "<div>To <a href=\"$tokenUrl\">unsubscribe</a> from these emails click here -> <a href=\"$tokenUrl\">Unsubscribe</a></div>"

        emailService.send(
            toAddress = EmailAddress(this.getEmail()!!),
            subject   = "Someone has replied in one of your discussions.",
            body      = "$mainMessage<BR/><BR/><BR/>$unsubscribeMessage"
        )
    }

    fun sendAccountConfirmationEmail(potentialEmail: String) {
        var token:Token        = this.addNewToken(email = potentialEmail)
        var tokenUrl: String   = applicationProperties.baseUrl + "/complete-signup?t=${token.value}"

        emailService.send(
            toAddress = EmailAddress(potentialEmail),
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendPasswordResetEmail() {
        var token:Token        = this.addNewToken(email = this.getEmail()!!)
        var tokenUrl: String   = applicationProperties.baseUrl + "/bloip-reset-my-password?t=${token.value}"
        emailService.send(
            toAddress = EmailAddress(this.getEmail()!!),
            subject   = "Reset my password",
            body      = "Click this link to reset your password <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendEmailResetEmail(potentialNewEmail: String) {
        var token:Token        = this.addNewToken(email = this.getEmail()!!)
        var tokenUrl: String   = applicationProperties.baseUrl + "/bloip-reset-my-email?t=${token.value}"
        emailService.send(
            toAddress = EmailAddress(potentialNewEmail),
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun showCompleteSignupView(model: Model, inputToken: String) : Boolean {
        val token: Token = tokensByToken[inputToken] ?: return false
        model["email"] = token.email
        model["token"] = token.value
        return true
    }

    fun delete() {
        userRepository.deleteById(this.id)
        userCache.deleteFromCache(user = this)
    }

    fun isDiscussionCreationLimitReached() : Boolean {
        return this.isDiscussionCreationLimitReached(
            applicationProperties.maxDiscussionCreationsPerDay
        )
    }
}