package com.example.springbatchtechnic.faulttolerance

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "skipJob")
@Configuration
class SkipJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory
) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun skipJob(): Job {
        return jobBuilderFactory["skipJob"].start(skipStep())
            .next(skipWithProcessorNonTransactionalStep())
            .build()
    }

    fun skipStep(): Step {
        return stepBuilderFactory["skipStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .skip(RuntimeException::class.java)
            .skipLimit(3)
            .build()
    }

    fun skipWithProcessorNonTransactionalStep(): Step {
        return stepBuilderFactory["skipWithProcessorNonTransactionalStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .skip(RuntimeException::class.java)
            .skipLimit(3)
            .processorNonTransactional()
            .build()
    }

    fun itemReader(): ItemReader<Int> {
        val queue: Queue<Int> = LinkedList(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        return ItemReader<Int> {
            val result = queue.poll()
            if (result == 2) {
                log.error("2 error! skip!")
                throw RuntimeException()
            }
            log.info("read : $result")
            result
        }
    }

    fun itemProcessor(): ItemProcessor<Int, Int> {
        return ItemProcessor<Int, Int> { input ->
            log.error("process receive $input!")
            if (input == 3) {
                log.error("3 error! skip!")
                throw RuntimeException()
            }
            val result = input * input
            log.info("process : input: $input, result: $result")
            result
        }
    }

    fun itemWriter(): ItemWriter<Int> {
        return ItemWriter<Int> { items ->
            log.info("writer receive items: $items")
            items.forEach { item ->
                if (item == 16) {
                    log.error("16 error! skip!")
                    throw RuntimeException()
                }
                log.info("write: $item")
            }
        }
    }
}



