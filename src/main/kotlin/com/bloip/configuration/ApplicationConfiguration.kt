package com.bloip.configuration

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
/*import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaDialect
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement*/
import org.thymeleaf.spring5.SpringTemplateEngine
import org.thymeleaf.spring5.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import javax.sql.DataSource


/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration(
    //@Autowired val dataSource: DataSource,
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

    /*@Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.jpaVendorAdapter = HibernateJpaVendorAdapter()
        em.setPackagesToScan("com.bloip.domain")
        em.persistenceUnitName = "mainPersistentUnit"
        return em
    }

    @Bean
    fun transactionManager(): PlatformTransactionManager? {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory().getObject()
        return transactionManager
    }*/
}
