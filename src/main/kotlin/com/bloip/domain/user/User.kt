package com.bloip.domain.user

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Country
import com.bloip.domain.user.authentication.UserAuthenticationDTO
import com.bloip.domain.user.authentication.Role
import com.bloip.domain.value.EmailAddress
import com.bloip.repositories.UserRepository
import com.bloip.services.*
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
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
import org.junit.jupiter.api.Assertions.*
import org.springframework.ui.ExtendedModelMap
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
@Suppress("UNCHECKED_CAST")
open class User : StandardDomainObject()
{
    @Service
    class UserService(
        @Autowired private val userRepository: UserRepository
    ) : UserDetailsService
    {
        override fun loadUserByUsername(username: String): UserAuthenticationDTO {
            return User.loadUserByUsername(username = username)
        }

        fun saveToTheDatabase(user: User) : User {
            return userRepository.save(user)
        }

        fun deleteFromTheDatabase(user: User) {
            userRepository.delete(user)
        }

        fun findAllFromDatabase() : MutableIterable<User> {
            return userRepository.findAll()
        }
    }

    @Component
    class SpringAdapter(
        @Autowired private val userService: UserService,
        @Autowired private val discussionService: DiscussionService,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val roleService: RoleService,
        @Autowired private val passwordEncoder: PasswordEncoder,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val emailService: EmailService,
        @Autowired private val userRepository: UserRepository
        //PersistenceContext private val entityManager: EntityManager
    ) {

        @PostConstruct
        fun init() {
            User.applicationProperties = applicationProperties
            User.roleService           = roleService
            User.passwordEncoder       = passwordEncoder
            User.emailService          = emailService
            User.userService           = userService
            User.discussionService     = discussionService
            User.userRepository        = userRepository
            //User.entityManager         = entityManager

            /** Initial set of users for pre-populating various caches **/
            val initialUsers: MutableIterable<User> = userService.findAllFromDatabase()

            println("Users found: " + initialUsers.count())
            for (u in initialUsers) {
                println("U: " + u.emailAddress)
            }

            /** Initialize the user cache **/
            loggingService.log("Initializing user cache")
            userCache = UserCache(
                applicationProperties = applicationProperties,
                initialUsers = initialUsers
            )
            loggingService.log("User cache initialized\r\n\r\n")

            /** Initialize the tokens **/
            Token.initializeTokens(initialUsers = initialUsers)

            /** Initialize cookie stores **/
            CookieInfo.init(initialUsers = initialUsers)

            /** Initialize inbox stores **/
            InboxItem.init(initialUsers = initialUsers)

            /** Create the root user if it doesn't exist **/
            if (!usernameExists(applicationProperties.shogunUsername)) {
                println("SHOGUN: " + findByUsername(applicationProperties.shogunUsername))
                createAShogun(
                    username = applicationProperties.shogunUsername,
                    password = applicationProperties.shogunPassword
                )
            }
        }
    }

    private class UserCache(private val applicationProperties: ApplicationProperties, initialUsers: Iterable<User>) {

        private val users: MutableMap<Long, User> = ConcurrentHashMap<Long, User>()
        private val usersByEmailAddress: MutableMap<String, User> = ConcurrentHashMap()

        init {
            for(u: User in initialUsers) {
                users[u.id] = u
                if (u.emailAddress != null && u.password != null) {
                    usersByEmailAddress[u.emailAddress!!.value] = u
                }
            }
        }

        fun findByIdFromCache(userId: Long) : User? {
            return users[userId]
        }

        fun findByUserNameFromCache(username: String) : User? {
            return usersByEmailAddress[username]
        }

        fun loadByUserNameFromCache(username: String) : User? {
            return usersByEmailAddress[username]
        }

        fun usernameExistsInCache(username: String) : Boolean {
            return usersByEmailAddress[username] != null
        }

        fun addToCache(user: User) : User {
            users[user.id] = user
            if (user.emailAddress != null) {
                usersByEmailAddress[user.getEmail()!!] = user
            }
            return user
        }

        fun deleteFromCache(user: User) {
            users.remove(user.id)
            if (user.getEmail() != null) {
                usersByEmailAddress.remove(user.getEmail())
            }
        }

        fun purgeEmail(email: String) {
            usersByEmailAddress.remove(email)
        }

        fun deleteAllFromCache() {
            if(applicationProperties.enableRemoteServices != "NO" && applicationProperties.environment != "dev") {
                throw RuntimeException("Delete all not allowed in environment")
            }
            for (u in users.values) {
                if(u.getEmail() != applicationProperties.shogunUsername) {
                    users.remove(key = u.id)
                    if(u.getEmail() != null && usersByEmailAddress.contains(u.getEmail())) {
                        usersByEmailAddress.remove(u.getEmail())
                    }
                }
            }
            /** If you don't comment this out, default users will be cleared **/
            //users.clear()
            //authenticationDetailsByUserName.clear()
        }

        fun findAllFromCache() : Collection<User> {
            return users.values
        }
    }
    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var roleService: RoleService
        private lateinit var userCache: UserCache
        private lateinit var passwordEncoder: PasswordEncoder
        private lateinit var emailService: EmailService
        private lateinit var userService: UserService
        private lateinit var discussionService: DiscussionService
        private lateinit var userRepository: UserRepository
        //private lateinit var entityManager: EntityManager

        fun createAShogun(username: String, password: String) : User {
            var rootUser: User = createNewUser()
            rootUser = rootUser.addAuthenticationDetails(
                email    = username,
                password = password
            )
            /** Give it every role **/
            for (r: Role in roleService.getRoles()) {
                rootUser.addRole(r)
            }
            return rootUser.save()
        }

        fun createNormalUser(username: String, password: String) : User {
            return createNewUser().addAuthenticationDetails(
                email    = username,
                password = password
            )
        }

        fun createNewUser(): User {
            return userCache.addToCache(
                User().save()
            )
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
            return CookieInfo.findUserByCode(code = code)
        }

        fun loadUserByUsername(username: String): UserAuthenticationDTO {
            val user: User = userCache.loadByUserNameFromCache(username = username)
                ?: throw UsernameNotFoundException("Username not found")

            return UserAuthenticationDTO(
                username = user.getEmail()!!,
                password = user.password!!,
                roles    = user.roles
            )
        }

        /*fun deleteAll() {
            userService.deleteAll()
        }*/

        fun deleteAll() {
            println("Calling delete ALLLLLLL!!!!!!!!")
            if (applicationProperties.environment != "dev") {
                throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
            }

            for (u in userRepository.findAll()) {
                println("Deleting user: ${u.id}")
                //if (u.getEmail() != applicationProperties.shogunUsername) {
                u.deleteFromTheDatabase()
                //}
            }
            userCache.deleteAllFromCache()
        }

        fun findAllFromDatabase() : Collection<User> {
            return userService.findAllFromDatabase() as Collection<User>
        }

        fun findAll() : Collection<User> {
            return userCache.findAllFromCache()
        }

        fun findByToken(tokenValue: String?): User? {
            return Token.findUserByToken(tokenValue)
        }

        fun showCompleteSignupView(
            model: Model,
            inputToken: String,
            onError: () -> Any,
            onSuccess: () -> Any
        ) : Any {
            return Token.showCompleteSignupView(
                model      = model,
                inputToken = inputToken,
                onError    = onError,
                onSuccess  = onSuccess
            )
        }

        fun changeEmailFromToken(tokenValue: String, success: ()-> Any, failure: () -> Any) : Any {
            return Token.updateUserEmailByToken(
                tokenValue = tokenValue,
                failure    = failure,
                success    = success
            )
        }

        fun completeSignupFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
            return Token.completeSignupFromToken(
                tokenValue = tokenValue,
                password   = password,
                success    = success,
                failure    = failure
            )
        }

        fun changePasswordFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
            return Token.changePasswordFromToken(
                tokenValue = tokenValue,
                password   = password,
                failure    = failure,
                success    = success
            )
        }

        /****** User inbox methods ********/
        fun updateSubscriberInboxes(sender: User, discussion: Discussion, trackNumber: Int, users: Set<User>) {
            InboxItem.updateSubscriberInboxes(
                sender      = sender,
                discussion  = discussion,
                trackNumber = trackNumber,
                users       = users
            )
        }

        fun testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User, numDiscussions: Int, defaultCountry: Country) {
            InboxItem.testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(
                userA          = userA,
                userB          = userB,
                numDiscussions = numDiscussions,
                defaultCountry = defaultCountry
            )
        }

        /** Walk the graph to verify content is paginated correctly in the bump stack **/
        fun testPaginateInbox(user: User, totalItems: Int, itemsPerPage: Int) {
            InboxItem.testPaginateInbox(
                user         = user,
                totalItems   = totalItems,
                itemsPerPage = itemsPerPage
            )
        }

        fun testCanToggleInboxSubscriptions(defaultCountry: Country) {
            InboxItem.testCanToggleInboxSubscriptions(
                defaultCountry = defaultCountry
            )
        }

        fun testDeleteInboxItemWithoutUnsubscribing(defaultCountry: Country) {
            InboxItem.testDeleteInboxItemWithoutUnsubscribing(defaultCountry = defaultCountry)
        }

        fun testVerifyDatabaseInboxTotal(total: Int) {
            InboxItem.testVerifyDatabaseInboxTotal(total)
        }

    }
    /**** End of static methods ************************************************************/

    /**** User instance fields and methods below *******************************************/

    private var censured:      Boolean = false
    private var censureDate:   Date?   = null
    private var emailDisabled: Boolean = false

    @Embedded
    private var emailAddress: EmailAddress? = null
    private var password: String? = null

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name               = "user_role",
        joinColumns        = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    private var roles: MutableSet<Role> = mutableSetOf()

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.ALL
        ],
        orphanRemoval = true,
        mappedBy = "user"
    )
    private val cookies: MutableSet<CookieInfo> = mutableSetOf()
    @Entity
    @Table(name = "cookie")
    private class CookieInfo : StandardDomainObject {
        @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = []
        )
        @JoinColumn(name="user_id", referencedColumnName = "id")
        private val user: User

        private val code: String

        constructor(user: User, code: String) {
            this.user = user
            this.code = code
        }

        @Version
        private val version = 0

        companion object {
            private val cookiesByCode: MutableMap<String, CookieInfo> = ConcurrentHashMap()

            fun init(initialUsers: Iterable<User>) {
                for(u in initialUsers) {
                    for(c in u.cookies) {
                        cookiesByCode[c.code] = c
                    }
                }
            }

            fun findByCode(user: User, code: String) : CookieInfo? {
                for(c in user.cookies) {
                    if(c.code == code) {
                        return c
                    }
                }
                return null
            }

            fun findUserByCode(code: String) : User? {
                return findById(cookiesByCode[code]?.user?.id)
            }

            fun saveCookieInfo(user: User, code: String) : User {
                user.cookies.add(
                    CookieInfo(
                        user = user,
                        code = code
                    )
                )
                val updatedUser = user.save()

                cookiesByCode[code] = findByCode(
                    user = updatedUser,
                    code = code
                )!!
                return updatedUser
            }

            fun deleteCookies(user: User) : User {
                val toBeDeleted: MutableList<CookieInfo> = mutableListOf()
                for(c in user.cookies) {
                    toBeDeleted.add(c)
                }
                for(d in toBeDeleted) {
                    cookiesByCode.remove(d.code)
                    user.cookies.remove(d)
                }
                return user.save()
            }
        }
    }

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.ALL
        ],
        orphanRemoval = true,
        mappedBy = "user"
    )
    private val tokens: MutableSet<Token> = mutableSetOf()
    @Entity
    @Table(name = "token")
    private class Token : StandardDomainObject {
        @ManyToOne(
            fetch   = FetchType.EAGER,
            cascade = []
        )
        @JoinColumn(name="user_id", referencedColumnName = "id")
        private val user: User

        @Column
        private val email: String
        @Column(name = "unsubscribe_token")
        private val unsubscribeToken: Boolean
        constructor(user: User, email: String, unsubscribeToken: Boolean = false) {
            this.user             = user
            this.email            = email
            this.unsubscribeToken = unsubscribeToken
        }

        @Column
        private val value: String = UUID.randomUUID().toString()

        @Column(name="creation_timestamp")
        private val creationTimestamp: Date = Date()

        fun getAccountCreationUrl() : String {
            return applicationProperties.baseUrl + "/complete-signup?t=${this.value}"
        }

        fun getPasswordResetUrl() : String {
            return applicationProperties.baseUrl + "/bloip-reset-my-password?t=${this.value}"
        }

        fun getUnsubscribeUrl() : String {
            return applicationProperties.baseUrl + "/unsubscribe-email?t=${this.value}"
        }

        fun getEmailResetUrl() : String {
            return applicationProperties.baseUrl + "/bloip-reset-my-email?t=${this.value}"
        }

        companion object {
            private val tokensByValue: MutableMap<String, Token> = ConcurrentHashMap()

            fun initializeTokens(initialUsers: Iterable<User>) {
                for(u in initialUsers) {
                    for (t in u.tokens) {
                        tokensByValue[t.value] = t
                    }
                }
            }
            fun findTokenByValue(tokenValue: String) : Token? {
                return tokensByValue[tokenValue]
            }

            fun findUserByToken(tokenValue: String?): User? {
                if(tokenValue == null) return null

                return findById(tokensByValue[tokenValue]?.user?.id) //This is done to avoid returning stale objects
            }

            fun updateUserEmailByToken(tokenValue: String, success: () -> Any, failure: () -> Any) : Any {
                val token: Token  = findTokenByValue(tokenValue = tokenValue) ?: return failure()
                val user: User    = findUserByToken(tokenValue = tokenValue)  ?: return failure()

                removeToken(
                    user  = user.updateEmail(newEmail = EmailAddress(token.email)),
                    token = token
                )
                return success()
            }

            fun completeSignupFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
                val token: Token = findTokenByValue(tokenValue = tokenValue)  ?: return failure()
                val user         = findUserByToken(tokenValue  = tokenValue)  ?: return failure()

                removeToken(
                    user  = user.addAuthenticationDetails(email = token.email, password = password),
                    token = token
                )
                return success()
            }

            fun changePasswordFromToken(tokenValue: String, password: String, success: ()-> Any, failure: () -> Any) : Any {
                val token: Token = findTokenByValue(tokenValue = tokenValue) ?: return failure()
                val user         = findUserByToken(tokenValue = tokenValue)  ?: return failure()

                removeToken(
                    user  = user.changePassword(password = password),
                    token = token
                )
                return success()
            }

            fun addNewToken(user: User, email: String, unsubscribeToken: Boolean = false) : Token {
                val token = Token(
                    user             = user,
                    email            = email,
                    unsubscribeToken = unsubscribeToken
                )
                user.tokens.add(token)
                user.save()

                tokensByValue[token.value] = token

                return token
            }

            fun getOrSetUnsubscribeToken(user: User, email: String) : Token {
                return findUnsubscribeToken(user)
                    ?:
                addNewToken(
                    user             = user,
                    email            = email,
                    unsubscribeToken = true
                )
            }

            fun findUnsubscribeToken(user: User) : Token? {
                for (t:Token in user.tokens) {
                    if(t.unsubscribeToken) {
                        return t
                    }
                }
                return null
            }

            fun removeToken(user: User, token: Token) : User {
                user.tokens.remove(token)
                val u = user.save()
                tokensByValue.remove(token.value)
                return u
            }

            fun showCompleteSignupView(
                model: Model,
                inputToken: String,
                onError: () -> Any,
                onSuccess: () -> Any
            ) : Any {

                val token: Token = tokensByValue[inputToken] ?: return onError()
                model["email"]   = token.email
                model["token"]   = token.value

                return onSuccess()
            }
        }
    }

    @Column(name="discussion_creation_count")
    private var discussionCreationCount: Int = 0
    @Column(name="first_discussion_creation_in_last_day")
    private var firstDiscussionCreationInLastDay: Date? = null
    @Version
    private val version = 0
    @Transient
    fun getEmail() : String? {
        return this.emailAddress?.value
    }
    private fun addRole(role: Role) {
        this.roles.add(role)
    }

    fun changePassword(password: String) : User {
        this.password = passwordEncoder.encode(password)
        return this.save()
    }

    fun addAuthenticationDetails(email: String, password: String) : User {
        this.emailAddress = EmailAddress(email)
        this.password     = passwordEncoder.encode(password)
        return this.save()
    }

    fun resetDiscussionCreationWindow() : User {
        this.discussionCreationCount = 0
        this.firstDiscussionCreationInLastDay = Date()
        return this.save()
    }

    private fun isEmailNotifiable() : Boolean {
        return ((this.emailAddress != null) && !this.emailDisabled)
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
        println("USER: ${this.id}, DCOUNT: ${this.discussionCreationCount}, MAX: $maxDiscussionCreationsPerDay")
        if (this.shouldResetCreationWindow()) {
            println("Creation window reset")
            return false
        }
        if (this.discussionCreationCount < maxDiscussionCreationsPerDay) {
            return false
        }
        println("Discussion limit reached for USER: ${this.id}")
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
    fun save(): User {
       return userCache.addToCache(
           saveToDatabase()
       )
    }

    /*** Lots of boilerplate needed to set up a session **/
    private fun saveToDatabase() : User {
        /*return EntityManagementUtils.saveToDatabase(
            entity = this,
            entityManager = User.entityManager,
            applicationProperties = applicationProperties
        )*/
        return userService.saveToTheDatabase(user = this)
    }

    private fun deleteFromTheDatabase() {
        userService.deleteFromTheDatabase(user = this)
    }

    fun resetCookies(code: String) : User {
        val updatedUser =  CookieInfo.deleteCookies(user = this)
        return CookieInfo.saveCookieInfo(user = updatedUser, code = code)
    }

    /** This needs to be used instead of save if you are trying to update an email otherwise the old cache index with the old email will be lying around **/
    fun updateEmail(newEmail: EmailAddress) : User {
        val oldEmail      = this.getEmail()!!
        this.emailAddress = newEmail
        val updatedUser   = this.saveToDatabase()

        userCache.purgeEmail(oldEmail)
        return userCache.addToCache(updatedUser)
    }
    fun sendDiscussionNotificationEmail() {
        if(!this.isEmailNotifiable()) return

        val inboxUrl               = applicationProperties.baseUrl + "/inbox"
        val unsubscribeUrl: String = Token.getOrSetUnsubscribeToken(user = this, email = this.getEmail()!!).getUnsubscribeUrl()

        val mainMessage            = "<div>Check your inbox to see new messages -> <a href=\"$inboxUrl\"> My Inbox </a></div>"
        val unsubscribeMessage     = "<div>To <a href=\"$unsubscribeUrl\">unsubscribe</a> from these emails click here -> <a href=\"$unsubscribeUrl\">Unsubscribe</a></div>"

        emailService.send(
            toAddress = EmailAddress(this.getEmail()!!),
            subject   = "Someone has replied in one of your discussions.",
            body      = "$mainMessage<BR/><BR/><BR/>$unsubscribeMessage"
        )
    }

    fun sendAccountConfirmationEmail(potentialEmail: String) {
        val accountCreationUrl: String = Token.addNewToken(user = this, email = potentialEmail).getAccountCreationUrl()

        emailService.send(
            toAddress = EmailAddress(potentialEmail),
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$accountCreationUrl\"> Click here </a>"
        )
    }

    fun sendPasswordResetEmail() {
        val passwordResetUrl: String = Token.addNewToken(user = this, email = this.getEmail()!!).getPasswordResetUrl()

        emailService.send(
            toAddress = EmailAddress(this.getEmail()!!),
            subject   = "Reset my password",
            body      = "Click this link to reset your password <a href=\"$passwordResetUrl\"> Click here </a>"
        )
    }

    fun sendEmailResetEmail(potentialNewEmail: String) {
        val tokenUrl: String = Token.addNewToken(user = this, email = potentialNewEmail).getEmailResetUrl()

        emailService.send(
            toAddress = EmailAddress(potentialNewEmail),
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun delete() {
        userService.deleteFromTheDatabase(this)
        userCache.deleteFromCache(user = this)
    }

    fun isDiscussionCreationLimitReached() : Boolean {
        return this.isDiscussionCreationLimitReached(
            applicationProperties.maxDiscussionCreationsPerDay
        )
    }

    fun resetUnreadConversationIndicator(discussion: Discussion) {
        InboxItem.resetUnreadConversationIndicator(user = this, discussion = discussion)
    }

    private fun getInboxTotal() : Int {
        return InboxItem.getInboxTotal(user = this)
    }

    fun toggleInboxSubscription(discussion: Discussion, value: Boolean) {
        InboxItem.toggleInboxSubscription(user = this, discussion = discussion, value = value)
    }

    open fun deleteConversation(discussion: Discussion) {
        InboxItem.deleteConversation(user = this, discussion = discussion)
    }

    private fun getNextPage(offsetKey: Discussion?) : BumpStack.Page<Long, InboxItem> {
        return InboxItem.getNextPage(user = this, offsetKey = offsetKey)
    }
    private fun getPreviousPage(offsetKey: Discussion) : BumpStack.Page<Long, InboxItem> {
        return InboxItem.getPreviousPage(user = this, offsetKey = offsetKey)
    }

    fun showInboxPage(model: Model, offset: Discussion?, direction: Int?) {
        val page: BumpStack.Page<Long, InboxItem> = if ( direction == null || direction >= 0 ) {
            this.getNextPage(offsetKey = offset)
        }  else {
            if (offset == null) {
                BumpStack.Page(previousOffsetKey = null, nextOffsetKey = null, values = emptyList())
            } else {
                this.getPreviousPage(offsetKey = offset)
            }
        }

        model["inbox"] = page.values
        WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
        WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)
    }

    fun testVerifyInboxTotal(inputValue: Int)  {
        assertEquals(inputValue, this.getInboxTotal())
    }

    fun showInboxTotal(httpServletResponse: HttpServletResponse) {
        httpServletResponse.writer.print(this.getInboxTotal())
    }

    fun setInboxTotalInSession(httpSession: HttpSession) {
        httpSession.setAttribute("inboxTotal", this.getInboxTotal())
    }

    /*** Inbox **********************************************/
    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [
            CascadeType.ALL
        ],
        orphanRemoval = true,
        mappedBy = "user"
    )
    private val inbox: MutableSet<InboxItem> = mutableSetOf()

    @Entity
    @Table(name = "inbox")
    private class InboxItem : StandardDomainObject{
        @ManyToOne(
            fetch = FetchType.LAZY,
            cascade = []
        )
        @JoinColumn(name="user_id", referencedColumnName = "id")
        private val user: User

        /*@ManyToOne(
            fetch = FetchType.EAGER,
            cascade = [
                CascadeType.PERSIST
            ]
        )
        @JoinColumn(name="discussion_id", updatable = false)
        private val discussion: Discussion*/
        @Column(name = "discussion_id")
        private val discussionId: Long

        @Embedded
        private val title: Title

        @Column(name="track_number")
        private val trackNumber: Int
        private var count: Int
        private var subscribed:Boolean
        @Column(name="last_update_timestamp")
        private var lastUpdateTimestamp: Date
        @Column(name="creation_timestamp")
        private val creationTimestamp: Date
        private var unread: Boolean

        @Version
        private var version:Int = 0

        constructor(user: User, discussionId: Long, title: Title, trackNumber: Int) {
            this.user                = user
            this.discussionId        = discussionId
            this.title               = title
            this.trackNumber         = trackNumber
            this.count               = 1
            this.lastUpdateTimestamp = Date()
            this.creationTimestamp   = Date()
            this.subscribed          = true
            this.unread              = true
        }

        override fun equals(other: Any?) : Boolean {
            if (other == null) {
                return false
            }
            return (this.user == (other as InboxItem).user) && (this.discussionId == other.discussionId)
        }

        override fun hashCode() : Int {
            return "${this.user.id}${this.discussionId}".hashCode()
        }

        private fun warrantsEmailNotification() : Boolean {
            return this.count == 1
        }

        companion object {
            private val inboxTotalsByUser: MutableMap<Long, Int?> = ConcurrentHashMap<Long, Int?>()
            private val inboxStackByUser: MutableMap<Long, BumpStack<Long, InboxItem>> = ConcurrentHashMap<Long, BumpStack<Long, InboxItem>>()

            fun init(initialUsers: Iterable<User>) {
                for(u: User in initialUsers) {
                    //Init inbox stack by User
                    val inbox: BumpStack<Long, InboxItem> = inboxStackByUser[u.id] ?: BumpStack()
                    for(i: InboxItem in u.inbox.sortedBy { x:InboxItem -> x.lastUpdateTimestamp }) {
                        println("InboxItem -> Discussion: ${i.discussionId}, title: ${i.title}")
                        inbox.push(key = i.discussionId, element = i)
                    }
                    inboxStackByUser[u.id]  = inbox
                    inboxTotalsByUser[u.id] = calculateInboxTotal(user = u)
                }
            }

            private fun calculateInboxTotal(user: User) : Int {
                val notes: List<InboxItem> = inboxStackByUser[user.id]?.getAll() ?: return 0
                return notes.fold(
                    0
                ) { acc, inboxItem -> acc + inboxItem.count }
            }

            fun updateSubscriberInboxes(sender: User, discussion: Discussion, trackNumber: Int, users: Set<User>) {
                for (user in users) {
                    if(user == sender) {
                        continue
                    }

                    updateInbox(
                        user         = user,
                        discussionId = discussion.id,
                        trackNumber  = trackNumber,
                        title        = discussion.title
                    )
                }
            }

            private fun updateInbox(user: User, discussionId: Long, trackNumber: Int, title: Title) {
                val inboxItem: InboxItem? = inboxStackByUser[user.id]?.get(key = discussionId)
                if (inboxItem == null) {
                    println("InboxUpdate.Create -> User: ${user.id}, Discussion: ${discussionId}, Title: $title")
                    createNewInboxConversation(
                        user      = user,
                        inboxItem = InboxItem(
                            user         = user,
                            discussionId = discussionId,
                            trackNumber  = trackNumber,
                            title        = title,
                        )
                    )
                } else {
                    println("InboxUpdate.bump -> User: ${user.id}, Discussion: ${discussionId}, Title: $title")
                    bumpExistingInboxConversationToTheTop(user = user, inboxItem = inboxItem)
                }

                /** Send notification email if applicable **/
                val updatedInboxItem: InboxItem = inboxStackByUser[user.id]!!.get(key = discussionId)!!
                if (updatedInboxItem.warrantsEmailNotification()) {
                    findById(userId = user.id)!!.sendDiscussionNotificationEmail()
                }
            }

            fun getInboxTotal(user: User) : Int {
                return inboxTotalsByUser[user.id] ?: 0
            }

            fun getNextPage(user: User, offsetKey: Discussion?) : BumpStack.Page<Long, InboxItem> {
                return inboxStackByUser[user.id]?.nextPage(inputKey = offsetKey?.id, N = applicationProperties.inboxItemsPerPage)
                    ?: return BumpStack.Page(null, null, emptyList())

            }
            fun getPreviousPage(user: User, offsetKey: Discussion) : BumpStack.Page<Long, InboxItem> {
                return inboxStackByUser[user.id]?.previousPage(inputKey = offsetKey.id, N = applicationProperties.inboxItemsPerPage)
                    ?: return BumpStack.Page(null, null, emptyList())
            }

            private fun findUpdatedInboxItem(user: User, inboxItem: InboxItem) : InboxItem? {
                for (i: InboxItem in user.inbox) {
                    if ( i == inboxItem) {
                        return i
                    }
                }
                return null
            }
            //TODO: Needs a transaction of some sort
            fun createNewInboxConversation(user: User, inboxItem: InboxItem) {
                user.inbox.add(inboxItem)
                val updatedUser:User = user.save()

                val inbox = inboxStackByUser[user.id] ?: BumpStack()
                if(inbox.size() == 0) {
                    inboxStackByUser[updatedUser.id] = inbox
                }
                inbox.push(key = inboxItem.discussionId, element = inboxItem)
                incrementUserInboxTotal(user = updatedUser)
            }

            private fun incrementUserInboxTotal(user: User) {
                inboxTotalsByUser[user.id] = (inboxTotalsByUser[user.id]?: 0) + 1
            }

            fun bumpExistingInboxConversationToTheTop(user: User, inboxItem: InboxItem) {
                //TODO: the repository update could be done in a future/promise
                inboxItem.count++
                user.inbox.add(inboxItem)
                val updatedUser = user.save()

                val inbox:BumpStack<Long, InboxItem> = inboxStackByUser[updatedUser.id]!!
                inbox.bump(
                    key = inboxItem.discussionId
                )
                incrementUserInboxTotal(user = updatedUser)
            }

            fun findInboxItemFromUser(user: User, discussion: Discussion) : InboxItem? {
                for(i in user.inbox) {
                    if(i.user == user && i.discussionId == discussion.id) {
                        return i
                    }
                }
                return null
            }
            fun deleteConversation(user: User, discussion: Discussion) {
                val inboxItem: InboxItem = findInboxItemFromUser(user = user, discussion = discussion) ?: return
                user.inbox.remove(inboxItem)
                val updatedUser = user.save()

                inboxStackByUser[updatedUser.id]?.remove(key = discussion.id)
                if (inboxTotalsByUser[updatedUser.id] != null) {
                    inboxTotalsByUser[updatedUser.id]  = inboxTotalsByUser[updatedUser.id]!! - inboxItem.count
                }
            }

            fun toggleInboxSubscription(user: User, discussion: Discussion, value: Boolean) {
                val inboxItem: InboxItem = findInboxItemFromUser(user = user, discussion = discussion) ?: return
                if (inboxItem.subscribed == value) {
                    return
                }

                inboxItem.subscribed = value
                val updatedUser = user.save()

                inboxStackByUser[updatedUser.id]!!.update(
                    key   = discussion.id,
                    value = findUpdatedInboxItem(user = updatedUser,inboxItem = inboxItem)!!
                )
            }

            fun resetUnreadConversationIndicator(discussion: Discussion, user: User) {
                val inboxItem: InboxItem = findInboxItemFromUser(user = user, discussion = discussion) ?: return
                val discussionCount: Int = inboxItem.count
                inboxItem.count  = 0
                inboxItem.unread = false
                reduceUserInboxTotal(user = user, count =  discussionCount)
                val updatedUser = user.save()

                inboxStackByUser[updatedUser.id]!!.update(
                    key   = discussion.id,
                    value = findInboxItemFromUser(user = updatedUser, discussion = discussion)!!
                )
             }

            fun reduceUserInboxTotal(user: User, count: Int) {
                inboxTotalsByUser[user.id] = inboxTotalsByUser[user.id]?.minus(count)
            }

            /*** Test helpers ***/
            fun testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User, numDiscussions: Int, defaultCountry: Country) {
                applicationProperties.inboxItemsPerPage = 11
                val discussions: MutableList<Discussion> = mutableListOf()

                for(i in 0 until numDiscussions) {
                    discussions.add(
                        discussionService.create(
                            user            = userA,
                            title           = Title("Why are raw oysters so expensive? $i"),
                            duration        = 20,
                            fileName        = "test.mp3",
                            country         = defaultCountry,
                            eventSequenceId = "sfsfsfsfs"
                        )
                    )
                }
                for (i in 0 until numDiscussions) {
                    discussionService.reply(
                        user            = userB,
                        discussion      = discussions[i],
                        duration        = 30,
                        fileName        = "test.mp3",
                        eventSequenceId = "sfsfsfsfs"
                    )
                }

                val model: Model = ExtendedModelMap()
                userA.showInboxPage(model = model, offset = null, direction = null)

                /** Extract the model data for testing. Has to be in this class or encapsulation is broken **/
                val userAInboxPageValues: List<InboxItem> = getInboxListFromModel(model)!!
                val inboxItemA = userAInboxPageValues[0]

                /** Respond directly to the discussions from the inbox and not implicitly from a list of discussion ids held in the test **/
                var lastDiscussionId = 0L

                userAInboxPageValues.forEach { x ->
                    lastDiscussionId = x.discussionId
                    discussionService.reply(
                        user            = userA,
                        discussion      = discussionService.get(discussionId = x.discussionId)!!,
                        duration        = 30,
                        fileName        = "test.mp3",
                        eventSequenceId = "dgdgdgdgd"
                    )
                }

                //TODO: Move this into it's own test
                /** Verify the discussion stack is updated/bumped correctly so the last reply discussion is first in the inbox
                 *  **/
                val pageResult = discussionService.getNextPage(country = defaultCountry, offsetKey = null).values
                assertTrue(pageResult.isNotEmpty())
                assertTrue(pageResult[0].id == lastDiscussionId)

                /** Verify User A and B's inbox total is updated correctly **/
                assertEquals(numDiscussions, userA.getInboxTotal())
                assertEquals(numDiscussions, userB.getInboxTotal())

                /**Verify inbox item info matches in both inboxes for the same conversation.
                 * The top item[0] that userA replies to first will appear at the bottom of user B's inbox
                 * **/
                val inboxItemB: InboxItem = getNextPage(
                    user      = userB,
                    offsetKey = null
                ).values[numDiscussions - 1]

                assertEquals(2, inboxItemA.trackNumber)
                assertEquals(3, inboxItemB.trackNumber)
                assertEquals(inboxItemA.discussionId, inboxItemB.discussionId)
                assertEquals(inboxItemA.count,inboxItemB.count)
                assertNotEquals(0,inboxItemB.count)
            }

            fun testPaginateInbox(user: User, totalItems: Int, itemsPerPage: Int) {
                /** From left to right **/
                var p = 0
                val numOfPages: Int = (totalItems / itemsPerPage) + (totalItems % itemsPerPage)
                var offsetKey: Long? = null

                var nextOffsetKey: Long?
                var previousOffsetKey: Long? = null

                var model: Model

                while(p < numOfPages) {
                    println("Seeking inbox page forward....")
                    model = ExtendedModelMap()
                    user.showInboxPage(model = model, offset = discussionService.get(discussionId = offsetKey), direction = null)
                    val inboxPageValues: List<InboxItem> = getInboxListFromModel(model)!!

                    nextOffsetKey     = model.getAttribute("nextOffsetKey") as Long?
                    previousOffsetKey = model.getAttribute("previousOffsetKey") as Long?

                    if(p < numOfPages - 1) {
                        //println("PageNumber: $p, NumPages: $numOfPages")
                        if(nextOffsetKey == null) {
                           println("PageNumber: $p, NumPages: $numOfPages, inboxItems: ${inboxPageValues.size}")
                           assertNotNull(nextOffsetKey)
                        }
                        assertNotNull(nextOffsetKey)
                    }
                    if (p > 0) {
                        //println("PageNumber: $p, NumPages: $numOfPages")
                        assertNotNull(previousOffsetKey)
                    }

                    /** Just verify the first element added is last. **/
                    if (p == numOfPages - 1) {
                        assertTrue(inboxPageValues[0].title.value.contains("0"))
                    }

                    offsetKey = nextOffsetKey
                    p++
                }

                /** From right to left **/
                /** Note - The previous function is exclusive to the starting offset whereas next is inclusive **/
                var x = 0
                while(previousOffsetKey != null) {
                    println("Seeking inbox page backward....")
                    model = ExtendedModelMap()
                    user.showInboxPage(model = model, offset = discussionService.get(discussionId = previousOffsetKey), direction = -1)

                    val inboxPageValues: List<InboxItem> = getInboxListFromModel(model)!!
                    nextOffsetKey     = model.getAttribute("nextOffsetKey") as Long?
                    previousOffsetKey = model.getAttribute("previousOffsetKey") as Long?

                    if (x > 0) {
                        assertNotNull(nextOffsetKey)
                    }
                    if (x < numOfPages - 2) {
                        assertNotNull(previousOffsetKey)
                    }

                    /** Just verify the first element added is last. **/
                    if (x == numOfPages - 2) {
                        assertTrue(inboxPageValues[0].title.value.contains("${totalItems - 1}"))
                    }
                    x++
                }
                assertEquals(numOfPages - 1, x)
            }

            fun testCanToggleInboxSubscriptions(defaultCountry: Country) {
                var userA = createNewUser()
                var discussion: Discussion = discussionService.create(
                    user            = userA,
                    title           = Title("Why are raw oysters so expensive?"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = "sfsfsfsfs"
                )
                userA = findById(userId = userA.id)!!
                val previousInboxTotal = userA.getInboxTotal()
                assertEquals(0, previousInboxTotal)

                discussion = discussionService.reply(
                    user            = createNewUser(),
                    discussion      = discussion,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "dgdgdgdgd"
                )
                userA = findById(userId = userA.id)!!

                val currentInboxTotal = userA.getInboxTotal()
                var model: Model = ExtendedModelMap()
                userA.showInboxPage(model = model, offset = null, direction = null)
                var inboxitem1: InboxItem = getInboxItemFromModel(model)!!
                assertEquals(discussion.id, inboxitem1.discussionId)

                assertEquals(1, currentInboxTotal)
                assertEquals(1, inboxitem1.count)



                discussionService.unsubscribe(
                    user       = userA,
                    discussion = discussionService.get(inboxitem1.discussionId)!!
                )
                discussion = discussionService.get(discussionId = inboxitem1.discussionId)!!

                discussionService.reply(
                    user            = createNewUser(),
                    discussion      = discussion,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "sfsfsfsfs"
                )
                userA = User.findById(userId = userA.id)!!
                assertEquals(1, userA.getInboxTotal())

                //Now resubscribe A, have user C send out a notification and confirm A's inbox is modified accordingly
                model = ExtendedModelMap()
                userA.showInboxPage(model = model, offset = null, direction = null)
                inboxitem1 = getInboxItemFromModel(model)!!

                discussionService.subscribe(
                    user       = userA,
                    discussion = discussionService.get(discussionId = inboxitem1.discussionId)!!
                )

                userA = findById(userId = userA.id)!!
                model = ExtendedModelMap()
                userA.showInboxPage(model = model, offset = null, direction = null)
                inboxitem1 = getInboxItemFromModel(model)!!

                discussionService.reply(
                    user            = createNewUser(),
                    discussion      = discussionService.get(discussionId = inboxitem1.discussionId)!!,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "sfssfsfsf"
                )

                userA = findById(userId = userA.id)!!
                model = ExtendedModelMap()
                userA.showInboxPage(model = model, offset = null, direction = null)
                inboxitem1 = getInboxItemFromModel(model)!!

                assertEquals(2, userA.getInboxTotal())
                assertEquals(2, inboxitem1.count)
            }

            fun testDeleteInboxItemWithoutUnsubscribing(defaultCountry: Country) {
                var user = createNewUser()
                var discussion: Discussion = discussionService.create(
                    user            = user,
                    title           = Title("Why are raw oysters so expensive?"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = "sfsfsfsfs"
                )
                discussion = discussionService.reply(
                    user            = createNewUser(),
                    discussion      = discussion,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "sfssfsfsf"
                )
                var model: Model = ExtendedModelMap()
                user = findById(user.id)!!
                user.showInboxPage(model = model, offset = null, direction = null)

                val inboxitem = getInboxItemFromModel(model)
                assertEquals(1, user.getInboxTotal())
                assertEquals(1, inboxitem!!.count)

                user.deleteConversation(discussion = discussionService.get(discussionId = inboxitem.discussionId)!!)
                model = ExtendedModelMap()
                assertEquals(0, findById(user.id)!!.getInboxTotal())
                assertNull(getInboxItemFromModel(model))

                discussionService.reply(
                    user            = createNewUser(),
                    discussion      = discussionService.get(discussionId = discussion.id)!!,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "sfssfs"
                )

                /** Confirm user is notified again with fresh request properties **/
                model = ExtendedModelMap()
                user = findById(user.id)!!
                user.showInboxPage(model = model, offset = null, direction = null)
               
                val latestInboxItem: InboxItem? = getInboxItemFromModel(model)
                assertEquals(1, user.getInboxTotal())
                assertEquals(1, latestInboxItem!!.count)
            }

            fun testVerifyDatabaseInboxTotal(total: Int) {
                var count = 0
                for (u: User in findAllFromDatabase()) {
                    count += u.getInboxTotal()
                }
                assertEquals(total, count)
            }

            private fun getInboxListFromModel(model: Model) : List<InboxItem>? {
               return (model.getAttribute("inbox") as List<InboxItem>?)
            }
            private fun getInboxItemFromModel(model: Model) : InboxItem? {
                val list = getInboxListFromModel(model) ?: return null
                if(list.isNotEmpty()) {
                    return list[0]
                }
                return null
            }
        }
    }

}