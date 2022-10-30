package com.bloip.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener


/**
 * Created by Usman Mutawakil on 10/29/22.
 */
@Configuration
class HttpSessionConfig {
    private val sessions: MutableMap<String, HttpSession> = HashMap()

    fun numOfSessions() : Int {
        return sessions.size
    }

    @Bean
    fun httpSessionListener(): HttpSessionListener {
        return object : HttpSessionListener {
            override fun sessionCreated(hse: HttpSessionEvent) {
                sessions[hse.session.id] = hse.session
            }

            override fun sessionDestroyed(hse: HttpSessionEvent) {
                sessions.remove(hse.session.id)
            }
        }
    }
}