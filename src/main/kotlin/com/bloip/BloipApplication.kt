package com.bloip

import com.bloip.configuration.ApplicationProperties
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class BloipApplication()


fun main(args: Array<String>) {
	println("Bloip starting.....")
	runApplication<BloipApplication>(*args)
	println("Bloip is fully loaded.")
}
