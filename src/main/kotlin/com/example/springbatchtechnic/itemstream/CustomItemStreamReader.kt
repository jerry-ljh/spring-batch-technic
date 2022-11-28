package com.example.springbatchtechnic.itemstream

import org.slf4j.LoggerFactory
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import java.util.*

open class CustomItemStreamReader : ItemStreamReader<Long> {
    companion object {
        const val OFFSET_IDX_NAME = "OFFSET_IDX"
    }

    private val log = LoggerFactory.getLogger(this::class.simpleName)
    private lateinit var queue: Queue<Long>
    private var offsetIdx = 0

    override fun open(executionContext: ExecutionContext) {
        offsetIdx = executionContext.get(OFFSET_IDX_NAME)?.toString()?.toInt()?.plus(1) ?: 0
        log.info("itemReaderStream call open ($OFFSET_IDX_NAME, $offsetIdx)")
        val initQueue: LinkedList<Long> = LinkedList(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
        queue = LinkedList(initQueue.subList(offsetIdx, initQueue.size))
    }

    override fun update(executionContext: ExecutionContext) {
        executionContext.put(OFFSET_IDX_NAME, offsetIdx)
        log.info("itemReaderStream call update($OFFSET_IDX_NAME, $offsetIdx)")
    }

    override fun close() {
        log.info("itemReaderStream call close")
    }

    override fun read(): Long? {
        val result = queue.poll()
        log.info("read : $result")
        if (result != null) offsetIdx++
        return result
    }
}