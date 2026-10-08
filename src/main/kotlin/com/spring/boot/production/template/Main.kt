package com.spring.boot.production.template

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class Main

fun main(args: Array<String>) {
	runApplication<Main>(*args)
}