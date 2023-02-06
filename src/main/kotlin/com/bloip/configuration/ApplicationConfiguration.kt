package com.bloip.configuration

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.util.Properties
import javax.sql.DataSource

/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val dataSource: DataSource,
    @Autowired val applicationProperties: ApplicationProperties
) {
    @Bean
    fun templateResolver() : ClassLoaderTemplateResolver {
        val secondaryTemplateResolver               =  ClassLoaderTemplateResolver()
        secondaryTemplateResolver.prefix            = "templates/"
        secondaryTemplateResolver.suffix            = ".html"
        secondaryTemplateResolver.templateMode      = TemplateMode.HTML
        secondaryTemplateResolver.characterEncoding = "UTF-8"
       // secondaryTemplateResolver.setOrder(1)
        //secondaryTemplateResolver.setCheckExistence(true)

        return secondaryTemplateResolver
    }

    @Bean
    fun templateEngine() : SpringTemplateEngine {
        val templateEngine = SpringTemplateEngine()
        templateEngine.setTemplateResolver(templateResolver())
        templateEngine.enableSpringELCompiler = true
        templateEngine.addDialect(LayoutDialect())
        return templateEngine
    }

    @Bean
    fun viewResolver() : ThymeleafViewResolver {
        val viewResolver = ThymeleafViewResolver()
        viewResolver.templateEngine = templateEngine()
        return viewResolver
    }

    @Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val em                 = LocalContainerEntityManagerFactoryBean()
        em.dataSource          = dataSource
        em.jpaVendorAdapter    = HibernateJpaVendorAdapter()
        em.persistenceUnitName = "mainPersistentUnit"
        em.setPackagesToScan("com.bloip.domain")

        val props = Properties()
        props["hibernate.dialect"]                       = applicationProperties.selectedHbmDialect
        props["hibernate.physical_naming_strategy"]      = "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        props["hibernate.implicit_naming_strategy"]      = "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
        props["hibernate.current_session_context_class"] = "org.springframework.orm.hibernate5.SpringSessionContext"
        props["hibernate.show_sql"]                      = false
        em.setJpaProperties(props)

        return em
    }

    @Bean
    fun transactionManager(): PlatformTransactionManager? {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory().nativeEntityManagerFactory
        return transactionManager
    }
}
