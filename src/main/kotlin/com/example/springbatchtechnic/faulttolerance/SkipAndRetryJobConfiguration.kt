package com.example.springbatchtechnic.faulttolerance

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.listener.SkipListenerSupport
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "skipAndRetryJob")
@Configuration
class SkipAndRetryJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory
) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun skipAndRetryJob(): Job {
        return jobBuilderFactory["skipAndRetryJob"].start(skipAndRetryStep())
            .next(skipAndRetryWithProcessorNonTransactionalStep())
            .build()
    }

    fun skipAndRetryStep(): Step {
        return stepBuilderFactory["skipAndRetryStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .skip(RuntimeException::class.java)
            .skipLimit(2)
            .retry(RuntimeException::class.java)
            .retryLimit(3)
            .listener(SkipListener())
            .build()
    }

    fun skipAndRetryWithProcessorNonTransactionalStep(): Step {
        return stepBuilderFactory["skipAndRetryWithProcessorNonTransactionalStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .faultTolerant()
            .skip(RuntimeException::class.java)
            .skipLimit(2)
            .retry(RuntimeException::class.java)
            .retryLimit(3)
            .processorNonTransactional()
            .listener(SkipListener())
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
            if (input == 3) {
                log.error("3 error! (tryCount: $tryCount)")
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
                if (item == 16) {
                    log.error("16 error! (tryCount: $tryCount)")
                    tryCount += 1
                    throw RuntimeException()
                }
                log.info("write: $item")
            }
        }
    }
}

class SkipListener<T : Any, S : Any> : SkipListenerSupport<T, S>() {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    override fun onSkipInProcess(item: T, t: Throwable) {
        log.error("process skip listen! data : $item")
        super.onSkipInProcess(item, t)
    }

    override fun onSkipInWrite(item: S, t: Throwable) {
        log.error("write skip listen! data : $item")
        super.onSkipInWrite(item, t)
    }
}