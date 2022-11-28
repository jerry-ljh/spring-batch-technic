package com.example.springbatchtechnic.async

import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.partition.PartitionHandler
import org.springframework.batch.core.partition.support.Partitioner
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.*

@ConditionalOnProperty(name = ["spring.batch.job.names"], havingValue = "partitioningJob")
@Configuration
class PartitioningJobConfiguration(
    private val jobBuilderFactory: JobBuilderFactory,
    private val stepBuilderFactory: StepBuilderFactory,
) {
    companion object {
        const val MIN = 1
        const val MAX = 25
    }

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    @Bean
    fun partitioningJob(): Job {
        return jobBuilderFactory["partitioningJob"]
            .start(managerStep())
            .build()
    }

    fun managerStep(): Step {
        return stepBuilderFactory["managerStep"]
            .partitioner("workerStep", partitioner())
            .step(workerStep())
            .partitionHandler(partitionHandler())
            .build()
    }

    fun workerStep(): Step {
        return stepBuilderFactory["workerStep"]
            .chunk<Int, Int>(5)
            .reader(itemReader(null, null))
            .writer(itemWriter())
            .build()
    }


    @Bean
    @StepScope
    fun itemReader(
        @Value("#{stepExecutionContext[start]}") start: Int?,
        @Value("#{stepExecutionContext[end]}") end: Int?
    ): ItemReader<Int> {
        log.info("[${Thread.currentThread().name}] read range : $start until $end")
        val queue: Queue<Int> = LinkedList((start!! until end!!).toList())
        return ItemReader<Int> {
            val result = synchronized(this) { queue.poll() }  // 동시성 문제
            log.info("[${Thread.currentThread().name}] read : $result")
            result
        }
    }

    fun itemWriter(): ItemWriter<Int> {
        return ItemWriter<Int> { items ->
            log.info("[${Thread.currentThread().name}] writer receive items: $items")
        }
    }

    fun partitioner(): Partitioner {
        return RangePartitioner(MIN, MAX)
    }

    fun partitionHandler(): PartitionHandler {
        val partitionHandler = TaskExecutorPartitionHandler()
        partitionHandler.step = workerStep()
        partitionHandler.setTaskExecutor(taskExecutor())
        partitionHandler.gridSize = 5
        return partitionHandler
    }

    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 20
        executor.maxPoolSize = 100
        executor.setThreadNamePrefix("partition_thread-")
        executor.initialize()
        return executor
    }
}

class RangePartitioner(
    private val min: Int,
    private val max: Int
) : Partitioner {

    override fun partition(gridSize: Int): MutableMap<String, ExecutionContext> {
        val targetSize = (max - min) / gridSize + 1
        val result: MutableMap<String, ExecutionContext> = HashMap()
        var number: Long = 0
        var start = 1
        var end = start + targetSize
        while (start <= max) {
            val value = ExecutionContext()
            result["partition$number"] = value
            value.putInt("start", start)
            value.putInt("end", end)
            start += targetSize
            end += targetSize
            number++
        }
        return result
    }
}