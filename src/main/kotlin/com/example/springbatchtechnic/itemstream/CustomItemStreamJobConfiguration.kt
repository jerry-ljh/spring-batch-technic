package com.example.springbatchtechnic.itemstream

import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "customItemStreamJob")
@Configuration
class CustomItemStreamJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory
) {

    @Bean
    fun customItemStreamJob(): Job {
        return jobBuilderFactory["customItemStreamJob"]
            .start(customItemStreamStep())
            .next(customItemStreamThrowStep())
            .build()
    }

    fun customItemStreamStep(): Step {
        return stepBuilderFactory["customItemStreamStep"]
            .chunk<Long, Long>(5)
            .reader(CustomItemStreamReader())
            .writer(CustomItemStreamWriter())
            .build()
    }

    fun customItemStreamThrowStep(): Step {
        return stepBuilderFactory["customItemStreamThrowStep"]
            .chunk<Long, Long>(5)
            .reader(CustomItemStreamThrowExceptionReader())
            .writer(CustomItemStreamWriter())
            .build()
    }
}