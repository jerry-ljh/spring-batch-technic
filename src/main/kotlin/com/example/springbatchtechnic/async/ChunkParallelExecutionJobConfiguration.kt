package com.example.springbatchtechnic.async

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
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.*


@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "chunkParallelExecutionJob")
@Configuration
class ChunkParallelExecutionJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun chunkParallelExecutionJob(): Job {
        return jobBuilderFactory["chunkParallelExecutionJob"]
            .start(chunkParallelExecutionStep())
            .build()
    }

    fun chunkParallelExecutionStep(): Step {
        return stepBuilderFactory["chunkParallelExecutionStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .taskExecutor(taskExecutor())
            .throttleLimit(5)
            .build()
    }

    fun itemReader(): ItemReader<Int> {
        val queue: Queue<Int> = LinkedList((1..25).toList())
        return ItemReader<Int> {
            val result = synchronized(this) { queue.poll() }  // 동시성 문제
            Thread.sleep(1000)
            log.info("[${Thread.currentThread().name}] read : $result")
            result
        }
    }

    fun itemProcessor(): ItemProcessor<Int, Int> {
        return ItemProcessor<Int, Int> { input ->
            val result = input * input
            Thread.sleep(1000)
            log.info("[${Thread.currentThread().name}] process : input: $input, result: $result")
            result
        }
    }

    fun itemWriter(): ItemWriter<Int> {
        return ItemWriter<Int> { items ->
            Thread.sleep(1000)
            log.info("[${Thread.currentThread().name}] writer receive items: $items")
        }
    }

    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 20
        executor.maxPoolSize = 100
        executor.setThreadNamePrefix("thread-")
        executor.initialize()
        return executor
    }
}