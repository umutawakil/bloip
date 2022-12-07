package com.bloip.configuration

import com.bloip.controllers.user.helpers.LoginSuccessHandler
import com.bloip.filters.GlobalFilter
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.session.SessionManagementFilter

/**
 * Created by Usman Mutawakil on 11/20/22.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
class SecurityConfig(
    @Autowired val loginSuccessHandler: LoginSuccessHandler,
    @Autowired val loggingService: LoggingService
) {
    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity) : SecurityFilterChain {
        return httpSecurity.addFilterBefore(GlobalFilter(loggingService), SessionManagementFilter::class.java).csrf().disable().authorizeRequests {
                authorizationConfig ->
            run {

                authorizationConfig.antMatchers(
                    "/castelo/**",
                    "/bloip-settings/**",
                    "/bloip-inbox/**"
                ).authenticated()
                authorizationConfig.anyRequest().permitAll()
            }
        }.
        sessionManagement().
        sessionCreationPolicy(
            SessionCreationPolicy.NEVER
        ).and()
        .formLogin().
        loginPage("/bloip-login").
        loginProcessingUrl("/bloip-login").
        successHandler(loginSuccessHandler).
        failureUrl("/bloip-login?error=1").
        and().
        logout().
        logoutUrl("/logout").
        deleteCookies(
            "rme",
            "JSESSIONID"
        ).
        invalidateHttpSession(false).
        logoutSuccessUrl("/bloip-login").
        and().
        build()
    }

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        return http.getSharedObject(AuthenticationManagerBuilder::class.java)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}