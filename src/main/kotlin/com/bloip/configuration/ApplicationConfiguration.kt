package com.bloip.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.web.server.MimeMappings
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.spring5.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import javax.sql.DataSource

/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration : WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    override fun customize(factory: ConfigurableServletWebServerFactory?) {
        val mappings =  MimeMappings(MimeMappings.DEFAULT)
        mappings.add("wasm", "application/wasm")
        factory!!.setMimeMappings(mappings)
    }

    @Bean
    fun templateResolver() : ClassLoaderTemplateResolver {
        val secondaryTemplateResolver =  ClassLoaderTemplateResolver()
        secondaryTemplateResolver.setPrefix("templates/")
        secondaryTemplateResolver.setSuffix(".html")
        secondaryTemplateResolver.setTemplateMode(TemplateMode.HTML)
        secondaryTemplateResolver.setCharacterEncoding("UTF-8")
       // secondaryTemplateResolver.setOrder(1)
        //secondaryTemplateResolver.setCheckExistence(true)

        return secondaryTemplateResolver;
    }

    /*@Bean
    fun dataSource() : DataSource {
        val configuration: HikariConfig = HikariConfig()
        configuration.poolName = "Bloip Hikari Connection Pool"
        /*
        autoCommit
connectionTimeout
idleTimeout
maxLifetime
connectionTestQuery
connectionInitSql
validationTimeout
maximumPoolSize
allowPoolSuspension
readOnly
transactionIsolation
leakDetectionThreshold
         */



        val dataSource: HikariDataSource = HikariDataSource()

        return dataSource
    } */

    /*@Bean
    fun templateResolver() : SpringResourceTemplateResolver {
        val templateResolver = SpringResourceTemplateResolver();
        //templateResolver.setPrefix("/WEB-INF/templates/")
        templateResolver.setSuffix(".html")
        templateResolver.setTemplateMode(TemplateMode.HTML)

        // Template cache is true by default. Set to false if you want
        // templates to be automatically updated when modified.
        templateResolver.setCacheable(true)
        return templateResolver
    }*/

    @Bean
    fun templateEngine() : SpringTemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.setTemplateResolver(templateResolver())
        // Enabling the SpringEL compiler with Spring 4.2.4 or newer can
        // speed up execution in most scenarios, but might be incompatible
        // with specific cases when expressions in one template are reused
        // across different data types, so this flag is "false" by default
        // for safer backwards compatibility.
        templateEngine.setEnableSpringELCompiler(true)
        return templateEngine
    }

    @Bean
    fun viewResolver() : ThymeleafViewResolver {
        val viewResolver = ThymeleafViewResolver()
        viewResolver.setTemplateEngine(templateEngine())
        return viewResolver
    }
}
