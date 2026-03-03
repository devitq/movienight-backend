package com.project.movienight

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.project.movienight.config")
class MovieNightApplication

fun main(args: Array<String>) {
    runApplication<MovieNightApplication>(*args)
}
