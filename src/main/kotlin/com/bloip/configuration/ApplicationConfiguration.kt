package com.bloip.configuration

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
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
class ApplicationConfiguration {
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
