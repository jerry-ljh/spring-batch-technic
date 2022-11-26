package com.example.springbatchtechnic

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableBatchProcessing
@SpringBootApplication
class SpringBatchTechnicApplication

fun main(args: Array<String>) {
	runApplication<SpringBatchTechnicApplication>(*args)
}
