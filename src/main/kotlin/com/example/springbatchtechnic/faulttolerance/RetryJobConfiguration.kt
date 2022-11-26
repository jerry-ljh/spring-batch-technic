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

@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "retryJob")
@Configuration
class RetryJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory
) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun retryJob(): Job {
        return jobBuilderFactory["retryJob"].start(retryStep())
            .next(retryWithProcessorNonTransactionalStep())
            .build()
    }

    fun retryStep(): Step {
        return stepBuilderFactory["retryStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .retry(RuntimeException::class.java)
            .retryLimit(3)
            .build()
    }

    fun retryWithProcessorNonTransactionalStep(): Step {
        return stepBuilderFactory["retryWithProcessorNonTransactionalStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .retry(RuntimeException::class.java)
            .retryLimit(3)
            .processorNonTransactional()
            .build()
    }

    fun itemReader(): ItemReader<Int> {
        val queue: Queue<Int> = LinkedList(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        return ItemReader<Int> {
            val result = queue.poll()
            log.info("read : $result")
            result
        }
    }

    fun itemProcessor(): ItemProcessor<Int, Int> {
        var tryCount = 1
        return ItemProcessor<Int, Int> { input ->
            log.error("process receive $input!")
            if (input == 3 && tryCount < 3) {
                log.error("3 error! retry! (tryCount: $tryCount)")
                tryCount += 1
                throw RuntimeException()
            }
            val result = input * input
            log.info("process : input: $input, result: $result")
            result
        }
    }

    fun itemWriter(): ItemWriter<Int> {
        var tryCount = 1
        return ItemWriter<Int> { items ->
            log.info("writer receive items: $items")
            items.forEach { item ->
                if (item == 16 && tryCount < 3) {
                    log.error("16 error! retry! (tryCount: $tryCount)")
                    tryCount += 1
                    throw RuntimeException()
                }
                log.info("write: $item")
            }
        }
    }
}



