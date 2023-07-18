package com.bloip.domain.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.localization.Language
import com.bloip.domain.user.authentication.UserAuthenticationDTO
import com.bloip.domain.user.authentication.Role
import com.bloip.domain.value.EmailAddress
import com.bloip.repositories.GenericRepository
import com.bloip.services.*
import com.bloip.services.admin.AdminService
import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.EntityManagementUtils
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.ui.Model
import org.springframework.ui.set
import java.util.*
import javax.annotation.PostConstruct
import javax.persistence.*
import java.io.Serializable
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
@Suppress("UNCHECKED_CAST")
class User
{
    @Service
    class UserService : UserDetailsService
    {
        override fun loadUserByUsername(username: String): UserAuthenticationDTO {
            return User.loadUserByUsername(username = username)
        }
    }

    @Component
    class SpringAdapter(
        @Autowired private val adminService: AdminService,
        @Autowired private val userService: UserService,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val passwordEncoder: PasswordEncoder,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val emailService: EmailService,
        @Autowired private val entityManagerFactory: EntityManagerFactory,
        @Autowired private val translationService: TranslationService,
        @Autowired private val genericRepository: GenericRepository,
        @Autowired private val pushService: PushService
    ) {

        @PostConstruct
        fun init() {
            User.genericRepository       = genericRepository
            User.adminService            = adminService
            User.loggingService          = loggingService
            User.applicationProperties   = applicationProperties
            User.passwordEncoder         = passwordEncoder
            User.emailService            = emailService
            User.userService             = userService
            User.entityManagerFactory    = entityManagerFactory
            User.translationService      = translationService
            User.pushService             = pushService
            maxDiscussionCreationsPerDay = applicationProperties.maxDiscussionCreationsPerDay

            /** Initial set of users for pre-populating various caches **/
            val initialUsers: Collection<User> = findAllFromDatabase()

            loggingService.log("Users found: " + initialUsers.count())
            for (u in initialUsers) {
                loggingService.log("U: " + u.emailAddress)
            }

            /** Initialize the user cache **/
            loggingService.log("Initializing user cache")
            userCache = UserCache(initialUsers = initialUsers)
            loggingService.log("User cache initialized\r\n\r\n")

            /** Initialize JWT code **/
            algorithm   = initAlgorithm()
            jwtVerifier = JWT.require(algorithm).withIssuer("bloip").build()

            /** Create the root user if it doesn't exist **/
            if (!usernameExists(applicationProperties.shogunUsername)) {
                createAShogun(
                    username = applicationProperties.shogunUsername,
                    password = applicationProperties.shogunPassword
                )
            }

            /** Load mobile device mappings **/
            MobileDevice.init()

            /** Account creation info **/
            AccountCreationTokenInfo.init()
        }
    }

    //TODO: Should re-evaluate whether its necessary to have t˙is as its own class and how useful will these locks really be?
    private class UserCache(initialUsers: Iterable<User>) {
        private val users: MutableMap<UserId, User>               = ConcurrentHashMap<UserId, User>()
        private val usersByEmailAddress: MutableMap<String, User> = ConcurrentHashMap()
        private val locks: MutableMap<UserId, Lock>               = ConcurrentHashMap<UserId, Lock> ()
        init {
            for(u: User in initialUsers) {
                users[u.id] = u
                locks[u.id] = ReentrantLock()
                if (u.emailAddress != null && u.password != null) {
                    usersByEmailAddress[u.emailAddress!!.value] = u
                }
            }
        }

        fun delete(u: User) {
            users.remove(u.id)
            locks.remove(u.id)
            if (u.getEmail() != null) {
                usersByEmailAddress.remove(u.getEmail())
            }
        }

        fun getLockFromCache(userId: UserId) : Lock {
            if(locks[userId] != null) {
                return locks[userId]!!
            }
            locks[userId] = ReentrantLock()
            return locks[userId]!!
        }

        fun findByIdFromCache(userId: UserId) : User? {
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

        /*fun purgeEmail(email: String) {
            usersByEmailAddress.remove(email)
        }*/

        fun findAllFromCache() : Collection<User> {
            return users.values
        }
    }
    companion object {
        private lateinit var genericRepository: GenericRepository
        private lateinit var adminService: AdminService
        private lateinit var loggingService: LoggingService
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var userCache: UserCache
        private lateinit var passwordEncoder: PasswordEncoder
        private lateinit var emailService: EmailService
        private lateinit var userService: UserService
        private lateinit var entityManagerFactory: EntityManagerFactory
        private lateinit var algorithm: Algorithm
        private lateinit var jwtVerifier: JWTVerifier
        private lateinit var translationService: TranslationService
        private lateinit var pushService: PushService

        var maxDiscussionCreationsPerDay:Int = 10 //Will be overwritten by properties file. Only a var for the purpose of unit testing.
        private fun initAlgorithm() : Algorithm {
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(applicationProperties.jwtKey.encodeToByteArray()))
            val privateKey: PrivateKey = keyFactory.generatePrivate(privateKeySpec)

            val publicKeySpec = RSAPublicKeySpec((privateKey as RSAPrivateKey).modulus, (privateKey as RSAPrivateCrtKey).publicExponent)
            val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)

            return Algorithm.RSA256(publicKey as RSAPublicKey, privateKey)
        }

        private fun getSession() : Session {
            return EntityManagementUtils.getSession(entityManagerFactory)
        }

        fun createAShogun(username: String, password: String) : User {
            val rootUser = User(email = username, password = password)

            /** Give it every role **/
            val roles: List<Role> = getSession().createQuery("SELECT r FROM Role r").resultList as List<Role>
            for (r: Role in roles) {
                rootUser.addRole(r)
            }
            return save(rootUser)
        }

        fun createNormalUser(username: String, password: String) : User {
            return save(
                User(
                    email    = username,
                    password = password
                )
            )
        }

        fun createNewUser(): User {
            return save(User())
        }

        fun save(user: User) : User {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val result = user.save(session)
                tx.commit()
                return result
            } finally {
                session.close()
            }
        }

        //TODO: Why can't this be used in more places?
        fun genericSave(entity: Any) : Any {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val result = session.merge(entity)
                tx.commit()
                return result
            } finally {
                session.close()
            }
        }

        fun genericDelete(entity: Any) {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            session.use { s ->
                s.delete(entity)
                tx.commit()
            }
        }

        fun findById(userId: UserId?): User? {
            if (userId == null) {
                return null
            }
            return userCache.findByIdFromCache(userId)
        }
        private fun getLock(userId: UserId): Any {
            return userCache.getLockFromCache(userId) //Most likely a stale userId being accessed. Downstream code will deal with it.
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

        fun loadUserByUsername(username: String): UserAuthenticationDTO {
            val user: User = userCache.loadByUserNameFromCache(username = username)
                ?: throw UsernameNotFoundException("Username not found")

            return UserAuthenticationDTO(
                username = user.getEmail()!!,
                password = user.password!!,
                roles    = user.roles
            )
        }

        fun isDiscussionCreationLimitReached(userId: UserId) : Boolean {
            val user: User = findById(userId = userId)!!
            return isDiscussionCreationLimitReached(user)
        }

        private fun isDiscussionCreationLimitReached(user: User) : Boolean {
            if (user.shouldResetCreationWindow()) {
                 return false
            }
            if (user.discussionCreationCount < maxDiscussionCreationsPerDay) {
                return false
            }
            return true
        }

        fun updateDiscussionLimitStats(session: Session, userId: UserId) {
            synchronized(getLock(userId)) {
                val user: User = findById(userId = userId)!!
                if (user.firstDiscussionCreationInLastDay == null) {
                    user.firstDiscussionCreationInLastDay = Date()
                    user.discussionCreationCount = 1
                    user.save(session)
                    return
                }
                user.discussionCreationCount++
                if (user.shouldResetCreationWindow()) {
                    user.resetDiscussionCreationWindow()
                }
                user.save(session)
            }
        }

        fun showEmail(userId: UserId, model: Model) {
            model["email"] = findById(userId)!!.getEmail()!!
        }

        fun updateNotificationStatus(userId: UserId, disabled: Boolean, model: Model) {
            synchronized(getLock(userId)) {
                val user = findById(userId = userId)!!
                user.emailDisabled = disabled
                save(user).showIfDisabled(model)
            }
        }

        fun showEmailStatus(userId: UserId, model: Model) {
            val user: User = findById(userId = userId)!!
            user.showIfDisabled(model)
        }

        fun createUserIdJwt(userId: UserId) : String {
            return encodeJwtTokenForEmail(
                JWT.create().
                withIssuer("bloip").
                withClaim("userId","$userId").
                sign(algorithm)
            )
        }

        fun getUserIdFromJwt(tokenValue: String) : UserId {
            return UserId(
                    getClaimFromToken(
                        name       = "userId",
                        tokenValue = tokenValue
                    ).asString().toLong()
            )
        }

        fun sendEmailResetEmail(userId: UserId, potentialNewEmail: String, language: Language) {
            val map: Map<String, String> = translationService.getTranslationMap(context = "email-messages",language)

            emailService.send(
                toAddress = EmailAddress(potentialNewEmail),
                subject   = map["confirm-email-address"]!!,
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>${map["click-this-link"]!!} <a href=\"${getEmailResetUrl(userId, potentialNewEmail)}\"> ${map["click-here"]!!} </a></body></html>"
            )
        }

        private fun getEmailResetUrl(userId: UserId, email: String) : String {
            val token = encodeJwtTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))

            return applicationProperties.baseUrl + "/bloip-reset-my-email?t=${token}"
        }

        fun sendDiscussionNotification(userId: UserId, language: Language) {
            println("SendDiscussionNotification")
            sendDiscussionNotificationEmail(
                userId   = userId,
                language = language
            )
            sendDiscussionPushNotification(
                userId   = userId,
                language = language
            )
        }

        private fun sendDiscussionNotificationEmail(userId: UserId, language: Language) {
            val user: User = findById(userId) ?: return
            if (!user.isEmailNotifiable()) return
            val email = user.getEmail()!!

            val unsubscribeUrl: String = getUnsubscribeEmailUrl(userId, email)

            val map: Map<String, String> = translationService.getTranslationMap(context = "email-messages",language)

            val inboxUrl    = applicationProperties.baseUrl + "/inbox"
            val mainMessage =
                "<div>${map["reply-message"]!!} -> <a href=\"${inboxUrl}\"> ${map["my-inbox"]!!} </a></div>"
            val unsubscribeMessage =
                "<div>${map["unsubscribe-message"]!!} -> <a href=\"$unsubscribeUrl\">${map["unsubscribe"]!!}</a></div>"

            emailService.send(
                toAddress = EmailAddress(email),
                subject   = map["reply-subject"]!!,
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>$mainMessage<BR/><BR/><BR/>$unsubscribeMessage</body></html>"
            )
        }

        private fun sendDiscussionPushNotification(userId: UserId, language: Language) {
            loggingService.log("Attempting to sendDiscusionPushNotfication")
            val user: User = findById(userId) ?: return
            if (user.pushNotificationsDisabled()) return

            val map: Map<String, String> = translationService.getTranslationMap(context = "email-messages",language)

            MobileDevice.sendPushNotification(userId = userId, subject = map["reply-subject"]!!, body = map["reply-message"]!!)
        }

        private fun getUnsubscribeEmailUrl(userId: UserId, email: String) : String {
            val token = encodeJwtTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))

            return "${applicationProperties.baseUrl}/unsubscribe-email?t=$token"
        }

        fun sendAccountConfirmationEmail(userId: UserId, potentialEmail: String, language: Language) {
            val map: Map<String, String> = translationService.getTranslationMap(context = "email-messages",language)
            val token:String             = getAccountCreationToken(userId = userId, email = potentialEmail)

            try {
                AccountCreationTokenInfo.createAccountTokenInfoRecord(token = token, email = potentialEmail)
            } catch (exception: Exception) {
                adminService.recordException(exception = exception)
            }

            emailService.send(
                toAddress = EmailAddress(potentialEmail),
                subject   = map["confirm-email-address"]!!,
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>${map["click-this-link"]!!} <a href=\"${getAccountCreationUrl(token)}\"> ${map["click-here"]!!} </a></body></html>"
            )
        }

        fun hasPendingConfirmationEmail(email: String) : Boolean {
            return AccountCreationTokenInfo.hasPendingConfirmationEmail(email = email)
        }

        /**TODO: This class was built in a hurry, in part to stop a jackass spammer. Some of the redundant lowercase logic could be cleaned up **/
        @Entity(name="AccountCreationTokenInfo")
        @Table(name = "account_creation_token_info")
        private class AccountCreationTokenInfo : StandardDomainObject {
            private val value: String
            private var used: Boolean = false
            private val email: String
            constructor(value: String, email: String) {
                this.value = value
                this.email = email
            }

            companion object {
                private val accountCreationTokenInfos: MutableMap<String, AccountCreationTokenInfo>       = ConcurrentHashMap()
                private val accountCreationTokenInfoByEmail: MutableMap<String, AccountCreationTokenInfo> = ConcurrentHashMap()

                fun init() {
                    val results: List<AccountCreationTokenInfo> = genericRepository.findAll(targetClass = AccountCreationTokenInfo::class.java)
                    loggingService.log("Total account creation tokens: " + results.size)
                    for(r in results) {
                        accountCreationTokenInfos[r.value]                   = r
                        accountCreationTokenInfoByEmail[r.email.lowercase()] = r
                    }
                    loggingService.log("Account creation tokens by token ->  " + accountCreationTokenInfos.size)
                    loggingService.log("Account creation tokens by email ->  " + accountCreationTokenInfoByEmail.size)
                }

                fun hasPendingConfirmationEmail(email: String) : Boolean {
                    return accountCreationTokenInfoByEmail[email.lowercase()] != null
                }

                fun createAccountTokenInfoRecord(token: String, email: String)  {
                    val record = saveAccountTokenInfo(token = AccountCreationTokenInfo(value = token, email = email))
                    accountCreationTokenInfos[token]                   = record
                    accountCreationTokenInfoByEmail[email.lowercase()] = record
                }
                fun updateTokenStatus(session: Session, token: String) {
                    val tokenInfo: AccountCreationTokenInfo = accountCreationTokenInfos[token] ?: return
                    tokenInfo.used = true

                    val record = saveAccountTokenInfo(session = session, tokenInfo)
                    accountCreationTokenInfos[token]                          = record
                    accountCreationTokenInfoByEmail[record.email.lowercase()] = record
                }

                private fun saveAccountTokenInfo(session: Session? = null, token: AccountCreationTokenInfo) : AccountCreationTokenInfo {
                    if(session != null) {
                        return session.merge(token) as AccountCreationTokenInfo
                    }

                    val newSession: Session = getSession()
                    val tx = newSession.beginTransaction()
                    try {
                        val result = newSession.merge(token) as AccountCreationTokenInfo
                        tx.commit()
                        return result

                    } finally {
                        newSession.close()
                    }
                }
            }
        }

        private fun getAccountCreationToken(userId: UserId, email: String) : String {
            return encodeJwtTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))
        }

        private fun getAccountCreationUrl(token: String) : String {
            return applicationProperties.baseUrl + "/complete-signup?t=$token"
        }

        fun sendPasswordResetEmail(userId: UserId, language: Language) {
            val map: Map<String, String> = translationService.getTranslationMap(context = "email-messages",language)

            emailService.send(
                toAddress = EmailAddress(findById(userId)!!.getEmail()!!),
                subject   = map["reset-my-password"]!!,
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>${map["reset-my-password-message"]!!} <a href=\"${getPasswordResetUrl(userId)}\"> ${map["click-here"]!!} </a></body></html>"
            )
        }
        private fun getPasswordResetUrl(userId: UserId) : String {
            val token = encodeJwtTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .sign(algorithm))
            return applicationProperties.baseUrl + "/bloip-reset-my-password?t=$token"
        }

        private fun encodeJwtTokenForEmail(token: String) : String {
            return Base64.getEncoder().encodeToString(token.encodeToByteArray())
            //return URLEncoder.encode(token, "UTF-8")
        }

        fun deleteAll() {
            if (applicationProperties.environment != "dev") {
                throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
            }
            for (u in findAll()) {
                if (u.getEmail() == applicationProperties.shogunUsername) {
                    continue
                }
                delete(userId = u.id)
            }
        }

        //TODO: Will need some try catch blocks in the future
        fun delete(userId: UserId) {
            val lock: Lock = getLock(userId) as Lock
            assert((lock as ReentrantLock).holdCount == 0)

            lock.lock()
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val user = findById(userId) ?: return
                session.delete(session.merge(user))
                //session.delete(user)
                session.flush()
                userCache.delete(user)
                MobileDevice.deleteByUserId(userId = user.id)
                //session.delete(user)
                tx.commit()

            } finally {
                lock.unlock()
                session.close()
            }
        }

        fun findAll() : Collection<User> {
            return userCache.findAllFromCache()
        }


        fun findUserIdFromToken(tokenValue: String): UserId {
            return UserId(
                getClaimFromToken(
                    name       = "userId",
                    tokenValue = tokenValue
                ).asString().toLong()
            )
        }
        private fun findEmailFromToken(tokenValue: String): String {
            return getClaimFromToken(
                name       = "email",
                tokenValue = tokenValue
            ).asString()
        }
        private fun getClaimFromToken(name: String, tokenValue: String) : Claim {
            val normalizedToken = String(Base64.getDecoder().decode(tokenValue.encodeToByteArray()))
            return try {
                val decodedJWT: DecodedJWT = jwtVerifier.verify(normalizedToken)
                decodedJWT.getClaim(name) ?: throw RuntimeException("No $name claim found for token $normalizedToken")

            } catch (e:JWTVerificationException) {
                throw RuntimeException("Unable to verify token: $normalizedToken")
            }
        }

        fun showCompleteSignupView(
            model: Model,
            inputToken: String,
            onError: () -> Any,
            onSuccess: () -> Any
        ) : Any {
            val email: String = findEmailFromToken(tokenValue = inputToken)
            val user: User?   = findByEmail(email)
            if(user!= null) {
                return onError()
            }

            model["email"] = email
            model["token"] = inputToken

            return onSuccess()

            /*return Token.showCompleteSignupView(
                model      = model,
                inputToken = inputToken,
                onError    = onError,
                onSuccess  = onSuccess
            )*/
        }

        fun changeEmailFromToken(tokenValue: String, success: ()-> Any) : Any {
            val userId: UserId = findUserIdFromToken(tokenValue = tokenValue)

            //TODO: What should happen here if the email update fails? Should we update the email and remove the token as one?
            synchronized(getLock(userId)) {
                val email: String = findEmailFromToken(tokenValue = tokenValue)
                val user: User = findById(userId)!!
                user.updateEmail(newEmail = EmailAddress(email))
                save(user)
                return success()
            }
        }

        fun completeSignupFromToken(tokenValue: String, password: String, success: ()-> Any) : Any {
            val userId = findUserIdFromToken(tokenValue = tokenValue)
            val email  = findEmailFromToken(tokenValue = tokenValue)

            synchronized(getLock(userId)) {
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    val user: User = findById(userId)!!
                    if (user.getEmail() != null) { /**Current UI flow should stop this from happening by detecting early the user already has an email**/
                        /** User reactivating already used jwt...**/

                        //TODO: Could this be cleaned up? Is this a real case? (An existing user forcing themself through signup again? Could be a real hack
                        println("Error: User with an existing email address is signing up again!!!!")

                        return success()
                    } else {
                        save(
                            user.withAuthenticationCredentials(email = email, password = password)
                        )
                        try {
                            AccountCreationTokenInfo.updateTokenStatus(session = session, token = tokenValue)
                        } catch (exception: Exception) {
                            adminService.recordException(exception = exception)
                        }
                        tx.commit()
                    }

                } finally {
                    session.close()
                }
                return success()
            }
        }


        fun changePasswordFromToken(tokenValue: String, password: String, success: ()-> Any) : Any {
            val userId: UserId = findUserIdFromToken(tokenValue = tokenValue)

            synchronized(getLock(userId)) {
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    findById(userId)!!.changePassword(password = password)
                    tx.commit()

                }  finally {
                    session.close()
                }
                return success()
            }
            /*return Token.changePasswordFromToken(
                tokenValue = tokenValue,
                password   = password,
                failure    = failure,
                success    = success
            )*/
        }

        /** //TODO: Why should the genericRepository not be used in this class **/
        fun findAllFromDatabase() : Collection<User> {
            //return genericRepository.findAll(User::class.java)
            val entityManager: EntityManager = entityManagerFactory.createEntityManager()
            try {
                return entityManager.createQuery("SELECT u FROM User u").resultList as List<User>
            } finally {
                entityManager.close()
            }
        }

        fun censorUser(userId: UserId) {
            val user: User = findById(userId = userId) ?: return
            user.censored    = true
            user.censorDate = Date()
            save(user)
        }

        fun getUserIdFromCookie(request: HttpServletRequest, response: HttpServletResponse) : UserId? {
            return CookieInfo.getUserIdFromCookie(request, response)
        }

        fun resetCookie(userId: UserId, request: HttpServletRequest, response: HttpServletResponse) {
            CookieInfo.resetCookie(userId,request, response)
        }

        fun bindMobileDevice(userId: UserId, deviceKey: String, deviceType: String) {
            MobileDevice.bindMobileDevice(
                userId     = userId,
                deviceKey  = deviceKey,
                deviceType = deviceType
            )
        }
        fun logout(userId: UserId, deviceKey: String?) {
            MobileDevice.logout(
                userId = userId,
                deviceKey = deviceKey
            )
        }
    }

    class CookieInfo {
        companion object {
            private const val RME_COOKIE_NAME: String = "rme"
            fun getUserIdFromCookie(request: HttpServletRequest, response: HttpServletResponse): UserId? {
                val cookie = findCookieByName(RME_COOKIE_NAME, request.cookies) ?: return null

                var userId: UserId? = null
                try {
                    userId = getUserIdFromJwt(tokenValue = cookie.value)
                } catch (e: Exception) {
                    deleteExistingRMECookiesFromResponse(
                        cookies = request.cookies,
                        response = response
                    )
                }
                if (findById(userId) == null) {
                    deleteExistingRMECookiesFromResponse(
                        cookies = request.cookies,
                        response = response
                    )
                    return null
                }
                return userId
            }

            fun resetCookie(userId: UserId, request: HttpServletRequest, response: HttpServletResponse) {
                deleteExistingRMECookiesFromResponse(
                    cookies = request.cookies,
                    response = response
                )

                val code: String = createUserIdJwt(userId)
                val cookie = Cookie(RME_COOKIE_NAME, code)
                cookie.secure = true
                cookie.isHttpOnly = true
                cookie.path = "/"
                cookie.domain = getDomain(request)
                cookie.maxAge = 60 * 60 * 24 * 365 * 10 // 10 year cookie

                response.addCookie(cookie)
            }

            private fun findCookieByName(name: String, cookies: Array<Cookie>?): Cookie? {
                if (cookies == null) {
                    return null
                }
                for (c in cookies) {
                    if (c.name.equals(name)) {//TODO: Needs to enforce unique keys or check them all or take the newest an delete the rest or something
                        return c
                    }
                }
                return null
            }

            private fun getDomain(request: HttpServletRequest): String {
                return request.serverName.replace(".*\\.(?=.*\\.)", "")
            }

            private fun deleteExistingRMECookiesFromResponse(cookies: Array<Cookie>?, response: HttpServletResponse) {
                if (cookies == null) {
                    return
                }
                for (c in cookies) {
                    if (c.name.equals(RME_COOKIE_NAME)) {
                        c.value = ""
                        c.maxAge = 0
                        response.addCookie(c)
                    }
                }
            }
        }
    }

    /**** End of static methods ************************************************************/

    @Entity(name="MobileDevice")
    @Table(name = "mobile_device")
    class MobileDevice : StandardDomainObject {
        private val userId: UserId
        private val deviceKey: String
        private val deviceType: String
        private val refreshCount: Int

        constructor(userId: UserId, deviceKey: String, deviceType: String, refreshCount: Int) {
            this.userId       = userId
            this.deviceKey    = deviceKey
            this.deviceType   = deviceType
            this.refreshCount = refreshCount
        }

        companion object {
            private val devicesByUserId: MutableMap<UserId, MutableList<MobileDevice>> = ConcurrentHashMap<UserId, MutableList<MobileDevice>>()
            private val userIdByDevice: MutableMap<String, UserId>                     = ConcurrentHashMap<String, UserId>()

            fun init() {
                val mobileDevices = genericRepository.findAll(targetClass = MobileDevice::class.java)
                for(m in mobileDevices) {
                    userIdByDevice[m.deviceKey] = m.userId
                    devicesByUserId.computeIfAbsent(m.userId) { mutableListOf() }.add(m)
                }
                loggingService.log("Mobile devices loaded: ${mobileDevices.size}")
            }
            fun bindMobileDevice(userId: UserId, deviceKey: String, deviceType: String) {
                /** Delete any device mapping that exists for this device key **/
                val currentUserId: UserId? = userIdByDevice[deviceKey]
                var refreshCount = 0
                if (currentUserId != null) {
                    val mobileDevices = devicesByUserId[currentUserId]
                    if ((mobileDevices != null) && (mobileDevices.size > 0)) {
                        val matchingDevices: List<MobileDevice> = mobileDevices.filter { it.deviceKey == deviceKey }
                        if (matchingDevices.isNotEmpty()) {
                            refreshCount = matchingDevices[0].refreshCount + 1
                            mobileDevices.remove(matchingDevices[0])
                            genericRepository.delete(matchingDevices[0], targetClass = MobileDevice::class.java)
                        }
                    }
                }

                /** Create a fresh record **/
                val mobileDevice = genericSave(
                        MobileDevice(
                            userId       = userId,
                            deviceKey    = deviceKey,
                            deviceType   = deviceType,
                            refreshCount = refreshCount
                    )
                ) as MobileDevice

                /** Update data structures **/
                userIdByDevice[deviceKey] = userId
                devicesByUserId.computeIfAbsent(userId) { mutableListOf() }.add(mobileDevice)
            }

            fun deleteByUserId(userId: UserId) {
                val devices: MutableList<MobileDevice>? = devicesByUserId[userId]
                if(devices != null) {
                    for(d in devices) {
                        userIdByDevice.remove(d.deviceKey)
                    }
                    devicesByUserId.remove(userId)
                }
            }

            fun logout(userId: UserId, deviceKey: String?) {
                if(deviceKey == null) return

                userIdByDevice.remove(deviceKey)
                val devices: MutableList<MobileDevice> = devicesByUserId[userId] ?: return
                var mobileDevice: MobileDevice? = null
                for(d in devices) {
                    if(d.deviceKey == deviceKey) {
                        mobileDevice = d
                    }
                }

                devices.removeIf { it.deviceKey == deviceKey}
                if (mobileDevice != null) {
                    genericRepository.delete(entity = mobileDevice, targetClass = MobileDevice::class.java)
                }
            }

            fun sendPushNotification(userId: UserId, subject: String, body: String) {
                val devices = devicesByUserId[userId]
                if(devices == null) {
                    loggingService.log("User: $userId has no devices")
                    return
                }
                loggingService.log("Devices found: " + devices.size)
                for(d in devices) {
                    //loggingService.log("Sending notification to ${d.deviceKey}")
                    pushService.send(deviceToken = d.deviceKey,subject = subject, body = body)
                }
            }

        }
    }

    /**** User instance fields and methods below *******************************************/

    @Embeddable
    class UserId : Serializable {
        @Column(name="user_id", insertable = true, updatable = false, nullable = false)
        var userId: Long

        constructor(userId: Long){
            this.userId = userId
        }

        override fun hashCode(): Int {
            return userId.hashCode()
        }
        override fun equals(other: Any?): Boolean {
            return userId == (other as UserId).userId
        }
        override fun toString(): String {
            return "$userId"
        }
    }

    constructor()
    constructor(email: String, password: String) {
       withAuthenticationCredentials(email = email, password = password)
    }

    private fun withAuthenticationCredentials(email: String, password: String) : User {
        this.emailAddress = EmailAddress(email)
        this.password     = passwordEncoder.encode(password)
        return this
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    var realId: Long = -1
    var id: UserId
        get(): UserId {
            return UserId(this.realId)
        }
        set(x) {
            this.realId = x.userId
        }

    private var censored:      Boolean = false
    private var censorDate:    Date?   = null
    private var emailDisabled: Boolean = false
    private var pushDisabled:  Boolean = false

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

    @Column(name="discussion_creation_count")
    private var discussionCreationCount: Int = 0
    @Column(name="first_discussion_creation_in_last_day")
    private var firstDiscussionCreationInLastDay: Date? = null
    @Version
    private val version = 0
    @Transient
    private fun getEmail() : String? {
        return this.emailAddress?.value
    }

    private fun addRole(role: Role) {
        this.roles.add(role)
    }

    fun changePassword(password: String) : User {
       this.password = passwordEncoder.encode(password)
       return save(this)
    }

    private fun resetDiscussionCreationWindow() : User {
        this.discussionCreationCount = 0
        this.firstDiscussionCreationInLastDay = Date()
        return this
    }

    private fun isEmailNotifiable() : Boolean {
        return ((this.emailAddress != null) && !this.emailDisabled)
    }

    private fun pushNotificationsDisabled() : Boolean {
        return this.pushDisabled
    }

    private fun shouldResetCreationWindow() : Boolean {
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

    private fun showIfDisabled(model: Model) {
        model["disabled"] = emailDisabled
    }

    fun showIfDiscussionCreationLimitReached(model: Model) {
        model["discussionCreationLimitReached"] = isDiscussionCreationLimitReached(this)
    }

    fun <T> doIfCensored(deny: ()-> T, allow: () -> T) : T {
        return if(this.censored) {
            deny()
        } else {
          allow()
        }
    }

    /** Imported service methods before **************************************/
    private fun save(session: Session): User {
       return userCache.addToCache(
           saveToDatabase(session)
       )
    }

    /*** Lots of boilerplate needed to set up a session **/
    private fun saveToDatabase(session: Session) : User {
        /*return EntityManagementUtils.saveToDatabase(
            entity = this,
            entityManager = User.entityManager,
            applicationProperties = applicationProperties
        )*/
        //return userService.saveToTheDatabase(user = this)
        val user =  session.merge(this) as User
        session.flush()
        return user
    }

    /** This needs to be used instead of save if you are trying to update an email otherwise the old cache index with the old email will be lying around **/
    private fun updateEmail(newEmail: EmailAddress) : User {
        this.emailAddress = newEmail
        return this
        /*
        val oldEmail      = this.getEmail()!!
        this.emailAddress = newEmail
        val updatedUser   = this.saveToDatabase(session)

        userCache.purgeEmail(oldEmail)
        return userCache.addToCache(updatedUser)*/
    }

    override fun equals(other: Any?) : Boolean {
        if (other == null) {
            return false
        }
        return this.id == (other as User).id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}