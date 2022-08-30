package com.bloip

import com.bloip.configuration.ApplicationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class BloipApplication

fun main(args: Array<String>) {
	println("Running Spring Boot Application....")
	runApplication<BloipApplication>(*args)
}
