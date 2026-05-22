package com.project.movienight.config

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {
    @Bean
    fun timedAspect(meterRegistry: MeterRegistry): TimedAspect = TimedAspect(meterRegistry)
}
