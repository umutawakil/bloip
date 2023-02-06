package com.bloip.domain.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.user.authentication.UserAuthenticationDTO
import com.bloip.domain.user.authentication.Role
import com.bloip.domain.value.EmailAddress
import com.bloip.services.*
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
        @Autowired private val userService: UserService,
        @Autowired private val applicationProperties: ApplicationProperties,
        @Autowired private val passwordEncoder: PasswordEncoder,
        @Autowired private val loggingService: LoggingService,
        @Autowired private val emailService: EmailService,
        @Autowired private val entityManagerFactory: EntityManagerFactory
    ) {

        @PostConstruct
        fun init() {
            User.applicationProperties   = applicationProperties
            User.passwordEncoder         = passwordEncoder
            User.emailService            = emailService
            User.userService             = userService
            User.entityManagerFactory    = entityManagerFactory
            maxDiscussionCreationsPerDay = applicationProperties.maxDiscussionCreationsPerDay

            /** Initial set of users for pre-populating various caches **/
            val initialUsers: Collection<User> = findAllFromDatabase()

            println("Users found: " + initialUsers.count())
            for (u in initialUsers) {
                println("U: " + u.emailAddress)
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
                println("SHOGUN: " + findByUsername(applicationProperties.shogunUsername))
                createAShogun(
                    username = applicationProperties.shogunUsername,
                    password = applicationProperties.shogunPassword
                )
            }
        }
    }

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

        fun purgeEmail(email: String) {
            usersByEmailAddress.remove(email)
        }

        fun findAllFromCache() : Collection<User> {
            return users.values
        }
    }
    companion object {
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var userCache: UserCache
        private lateinit var passwordEncoder: PasswordEncoder
        private lateinit var emailService: EmailService
        private lateinit var userService: UserService
        private lateinit var entityManagerFactory: EntityManagerFactory
        private lateinit var algorithm: Algorithm
        private lateinit var jwtVerifier: JWTVerifier

        var maxDiscussionCreationsPerDay:Int = 10 //Will be overrieded by properties file. Only a var for the purpose of unit testing.
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
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                var rootUser: User = createNewUser()
                rootUser = rootUser.addAuthenticationDetails(
                    email = username,
                    password = password
                )

                /** Give it every role **/
                val roles: List<Role> = getSession().createQuery("SELECT r FROM Role r").resultList as List<Role>
                for (r: Role in roles) {
                    rootUser.addRole(r)
                }
                val updatedUser = rootUser.save(session)
                tx.commit()

                return updatedUser
            } finally {
                session.close()
            }
        }

        fun createNormalUser(username: String, password: String) : User {
            return createNewUser().addAuthenticationDetails(
                email    = username,
                password = password
            )
        }

        fun createNewUser(): User {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val result = userCache.addToCache(
                    User().save(session)
                )
                tx.commit()
                return result
            } finally {
                session.close()
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
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    val user = findById(userId = userId)!!
                    user.emailDisabled = disabled
                    (user.save(session)).showIfDisabled(model)
                    tx.commit()
                } finally {
                    session.close()
                }
            }
        }

        fun showEmailStatus(userId: UserId, model: Model) {
            val user: User = findById(userId = userId)!!
            user.showIfDisabled(model)
        }

        fun createUserIdJwt(userId: UserId) : String {
            return JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .sign(algorithm)
        }

        fun getUserIdFromJwt(tokenValue: String) : UserId {
            return UserId(
                    getClaimFromToken(
                        name       = "userId",
                        tokenValue = tokenValue
                    ).asString().toLong()
            )
        }

        fun sendEmailResetEmail(userId: UserId, potentialNewEmail: String) {
            emailService.send(
                toAddress = EmailAddress(potentialNewEmail),
                subject   = "Confirm email address",
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>Click this link to confirm your email address <a href=\"${getEmailResetUrl(userId, potentialNewEmail)}\"> Click here </a></body></html>"
            )
        }

        private fun getEmailResetUrl(userId: UserId, email: String) : String {
            val token = encodeTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))

            return applicationProperties.baseUrl + "/bloip-reset-my-email?t=${token}"
        }

        fun sendDiscussionNotificationEmailIfUserShouldBeEmailed(userId: UserId) {
            val user: User = findById(userId) ?: return
            if (!user.isEmailNotifiable()) return
            val email = user.getEmail()!!

            val unsubscribeUrl: String = getUnsubscribeEmailUrl(userId, email)

            val inboxUrl = applicationProperties.baseUrl + "/inbox"
            val mainMessage =
                "<div>Check your inbox to see new messages -> <a href=\"${inboxUrl}\"> My Inbox </a></div>"
            val unsubscribeMessage =
                "<div>To <a href=\"$unsubscribeUrl\">unsubscribe</a> from these emails click here -> <a href=\"$unsubscribeUrl\">Unsubscribe</a></div>"

            emailService.send(
                toAddress = EmailAddress(email),
                subject   = "Someone has replied in one of your discussions.",
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>$mainMessage<BR/><BR/><BR/>$unsubscribeMessage</body></html>"
            )
        }

        private fun getUnsubscribeEmailUrl(userId: UserId, email: String) : String {
            val token = encodeTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))

            return "${applicationProperties.baseUrl}/unsubscribe-email?t=$token"
        }

        fun sendAccountConfirmationEmail(userId: UserId, potentialEmail: String) {
            emailService.send(
                toAddress = EmailAddress(potentialEmail),
                subject   = "Confirm email address",
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>Click this link to confirm your email address <a href=\"${getAccountCreationUrl(userId, potentialEmail)}\"> Click here </a></body></html>"
            )
        }

        private fun getAccountCreationUrl(userId: UserId, email: String) : String {
            val token = encodeTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .withClaim("email",email)
                .sign(algorithm))

            return applicationProperties.baseUrl + "/complete-signup?t=$token"
        }

        fun sendPasswordResetEmail(userId: UserId) {
            emailService.send(
                toAddress = EmailAddress(findById(userId)!!.getEmail()!!),
                subject   = "Reset my password",
                body      = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>Click this link to reset your password <a href=\"${getPasswordResetUrl(userId)}\"> Click here </a></body></html>"
            )
        }
        private fun getPasswordResetUrl(userId: UserId) : String {
            val token = encodeTokenForEmail(JWT.create()
                .withIssuer("bloip")
                .withClaim("userId","$userId")
                .sign(algorithm))
            return applicationProperties.baseUrl + "/bloip-reset-my-password?t=$token"
        }

        private fun encodeTokenForEmail(token: String) : String {
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
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    val email: String = findEmailFromToken(tokenValue = tokenValue)
                    findById(userId)?.updateEmail(session = session, newEmail = EmailAddress(email))
                    tx.commit()

                    return success()
                } finally {
                    session.close()
                }
            }

            /*return Token.updateUserEmailByToken(
                tokenValue = tokenValue,
                failure    = failure,
                success    = success
            )*/
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
                        println("User reactivating already used jwt...")
                        return success()
                    } else {
                        user.addAuthenticationDetails(email = email, password = password)
                        tx.commit()
                    }

                } finally {
                    session.close()
                }
                return success()
            }

            /*return Token.completeSignupFromToken(
                tokenValue = tokenValue,
                password   = password,
                success    = success,
                failure    = failure
            )*/
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
    }
    /**** End of static methods ************************************************************/

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
        val session: Session = getSession()
        val tx = session.beginTransaction()
        try {
            this.password = passwordEncoder.encode(password)
            val user = this.save(session)
            tx.commit()
            return user
        } finally {
            session.close()
        }
    }

    fun addAuthenticationDetails(email: String, password: String) : User {
        val session: Session = getSession()
        val tx = session.beginTransaction()
        try {
            this.emailAddress = EmailAddress(email)
            this.password = passwordEncoder.encode(password)
            val result =  this.save(session)
            tx.commit()
            return result

        } finally {
            session.close()
        }
    }

    private fun resetDiscussionCreationWindow() : User {
        this.discussionCreationCount = 0
        this.firstDiscussionCreationInLastDay = Date()
        return this
    }

    private fun isEmailNotifiable() : Boolean {
        return ((this.emailAddress != null) && !this.emailDisabled)
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

    fun censureUser() : User {
        val session: Session = getSession()
        val tx = session.beginTransaction()
        try {
            this.censured = true
            this.censureDate = Date()
            val user = this.save(session)
            tx.commit()
            return user

        } finally {
            session.close()
        }
    }

    fun <T> doIfCensured(deny: ()-> T, allow: () -> T) : T {
        return if(this.censured) {
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
    private fun updateEmail(session: Session, newEmail: EmailAddress) : User {
        val oldEmail      = this.getEmail()!!
        this.emailAddress = newEmail
        val updatedUser   = this.saveToDatabase(session)

        userCache.purgeEmail(oldEmail)
        return userCache.addToCache(updatedUser)
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