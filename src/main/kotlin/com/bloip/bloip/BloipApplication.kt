package com.bloip.bloip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BloipApplication

fun main(args: Array<String>) {
	runApplication<BloipApplication>(*args)
}
