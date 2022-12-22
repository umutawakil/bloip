package com.bloip.services

import com.bloip.domain.value.EmailAddress
import com.bloip.domain.Token
import com.bloip.domain.user.User
import com.bloip.repositories.TokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Service
class UserTokenService(
    @Autowired private val tokenRepository: TokenRepository
) {

    val MAX_EMAILS = 5

    class TokenResult(val token: Token?, val limitReached: Boolean)
    private val userTokens: MutableMap<String, Token> = ConcurrentHashMap()
    private val tokensByEmail: MutableMap<String, MutableList<Token>> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        for (t: Token in tokenRepository.findAll()) {
            userTokens[t.value] = t
            tokensByEmail.putIfAbsent(t.email, mutableListOf())?.add(t)
        }
    }

    fun generateUserAccountToken(user: User?, email: EmailAddress) : TokenResult {
        return generateToken(user = user, email = email, isUnsubscribeToken = false)
    }

    fun generateUnsubscribeToken(user: User) : TokenResult {
        return generateToken(user = user, email = EmailAddress(user.authenticationUserDetail!!.username), isUnsubscribeToken = true)
    }

    private fun generateToken(user: User?, email: EmailAddress, isUnsubscribeToken: Boolean) : TokenResult {
        pruneOldTokensForThisEmail(email.value)
        if (emailLimitReached(email) && (!isUnsubscribeToken)) {
            return TokenResult(null, true)
        }

        val token: Token = tokenRepository.save(
            Token(
                user = user,
                email = email.value
            )
        )
        userTokens[token.value] = token
        tokensByEmail.putIfAbsent(token.email, mutableListOf(token))?.add(token)

        return TokenResult(token, false)
    }

    fun getToken(token: String?) : Token? {
        if (token == null) return null
        return userTokens[token]
    }

    fun remove(token: Token) {
        userTokens.remove(token.value)
        tokensByEmail[token.email]?.remove(token)
        tokenRepository.deleteById(token.id)
    }

    fun pruneOldTokensForThisEmail(email: String) {
        val tokens: MutableList<Token>? = tokensByEmail[email]
        if (tokens == null || tokens.size == 0) return

        var toDelete: MutableList<Token> = mutableListOf()
        for (t: Token in tokens) {
            val diffHours: Long = ((Date().time - t.creationTimestamp.time) / 1000) / 3600
            if (diffHours >= 24) {
                toDelete.add(t)
            }
        }
        toDelete.forEach {
            tokens.remove(it)
            tokenRepository.deleteById(it.id)
        }
    }

    fun emailLimitReached(email: EmailAddress): Boolean {
        if (tokensByEmail[email.value] == null || tokensByEmail[email.value]!!.size < MAX_EMAILS) { return false}

        return true
    }

    fun clearAll() {
        tokensByEmail.clear()
        userTokens.clear()
        tokenRepository.deleteAll()
    }
}