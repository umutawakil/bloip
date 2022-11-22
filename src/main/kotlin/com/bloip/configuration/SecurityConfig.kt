package com.bloip.configuration

import com.bloip.filters.CorsFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.session.SessionManagementFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


/**
 * Created by Usman Mutawakil on 11/20/22.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
class SecurityConfig(
    @Autowired private val applicationProperties: ApplicationProperties
) {

    /** This is because the admin functions fire off requests to controllers under different directories from the
     * homepage and VD page. This causes the browser (or some perhaps) to treat the request as a CORS request
     * even though the origin is the same. Biza
     ***/
    /*@Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins   = listOf(applicationProperties.baseUrl)
        configuration.allowedMethods   = listOf("GET", "POST")
        configuration.allowedHeaders   = listOf("Authorization")
        configuration.allowCredentials = true
        configuration.exposedHeaders   = listOf("Access-Control-Allow-Origin")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }*/*/

    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity) : SecurityFilterChain {
        //return httpSecurity.addFilterBefore(CorsFilter(), SessionManagementFilter::class.java).authorizeRequests {
        return httpSecurity.csrf().disable().authorizeRequests {
                authorizationConfig ->
            run {
                authorizationConfig.antMatchers("/castelo/**").authenticated()
                authorizationConfig.anyRequest().permitAll()
            }
        }.formLogin(Customizer.withDefaults()).build()
    }

    @Bean
    fun userDetailService() : UserDetailsService  {
        return InMemoryUserDetailsManager(
            User.builder().
            username(applicationProperties.shogunUsername).
            password(passwordEncoder().encode(applicationProperties.shogunPassword)).
            roles("SHOGUN","DAIMYO","SAMURAI").
            build(),
            User.builder().
            username("test").
            password(passwordEncoder().encode("test")).
            roles("SAMURAI").
            build()
        )
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}