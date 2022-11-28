package com.example.springbatchtechnic.async

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.integration.async.AsyncItemProcessor
import org.springframework.batch.integration.async.AsyncItemWriter
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.*
import java.util.concurrent.Future


@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "chunkAndItemProcessorAsyncJob")
@Configuration
class ChunkAndItemProcessorAsyncJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun chunkAndItemProcessorAsyncJob(): Job {
        return jobBuilderFactory["chunkAndItemProcessorAsyncJob"]
            .start(chunkAndItemProcessorAsyncStep())
            .build()
    }

    fun chunkAndItemProcessorAsyncStep(): Step {
        return stepBuilderFactory["chunkAndItemProcessorAsyncStep"]
            .chunk<Int, Future<Int>>(5)
            .reader(itemReader())
            .processor(asyncItemProcessor())
            .writer(asyncItemWriter())
            .taskExecutor(taskExecutor2())
            .throttleLimit(5)
            .build()
    }

    fun itemReader(): ItemReader<Int> {
        val queue: Queue<Int> = LinkedList((1..25).toList())
        return ItemReader<Int> {
            val result = queue.poll()
            log.info("[${Thread.currentThread().name}] read : $result, queue size: ${queue.size}")
            result
        }
    }


    fun itemProcessor(): ItemProcessor<Int, Int> {
        return ItemProcessor<Int, Int> { input ->
            Thread.sleep(1000)
            val result = input * input
            log.info("[${Thread.currentThread().name}] process : input: $input, result: $result")
            result
        }
    }

    fun asyncItemProcessor(): AsyncItemProcessor<Int, Int> {
        val asyncItemProcessor = AsyncItemProcessor<Int, Int>()
        asyncItemProcessor.setDelegate(itemProcessor())
        asyncItemProcessor.setTaskExecutor(taskExecutor())
        return asyncItemProcessor
    }

    fun itemWriter(): ItemWriter<Int> {
        return ItemWriter<Int> { items ->
            log.info("[${Thread.currentThread().name}] writer receive items: $items")
        }
    }

    fun asyncItemWriter(): AsyncItemWriter<Int> {
        val asyncItemWriter: AsyncItemWriter<Int> = AsyncItemWriter()
        asyncItemWriter.setDelegate(itemWriter())
        return asyncItemWriter
    }

    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 20
        executor.maxPoolSize = 100
        executor.setThreadNamePrefix("async_processor-")
        executor.initialize()
        return executor
    }

    fun taskExecutor2(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 20
        executor.maxPoolSize = 100
        executor.setThreadNamePrefix("chunk_thread-")
        executor.initialize()
        return executor
    }
}