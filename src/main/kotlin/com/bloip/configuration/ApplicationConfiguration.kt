package com.bloip.configuration

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.boot.web.server.MimeMappings
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration: WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    override fun customize(factory: ConfigurableServletWebServerFactory?) {
        val mappings =  MimeMappings(MimeMappings.DEFAULT)
        mappings.add("wasm", "application/wasm")
        factory!!.setMimeMappings(mappings)
    }

        /*@Bean
    fun dataSource() : DataSource {
        val configuration: HikariConfig = HikariConfig()
        configuration.poolName = "Bloip Hikari Connection Pool"
        val dataSource: HikariDataSource = HikariDataSource()

        return dataSource
    }*/

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

    @Bean
    fun templateEngine() : SpringTemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.setTemplateResolver(templateResolver())
        templateEngine.setEnableSpringELCompiler(true)
        templateEngine.addDialect(LayoutDialect())
        return templateEngine
    }

    @Bean
    fun viewResolver() : ThymeleafViewResolver {
        val viewResolver = ThymeleafViewResolver()
        viewResolver.setTemplateEngine(templateEngine())
        return viewResolver
    }
}
