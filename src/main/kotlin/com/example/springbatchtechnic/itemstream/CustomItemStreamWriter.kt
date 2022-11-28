package com.example.springbatchtechnic.itemstream

import org.slf4j.LoggerFactory
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamWriter
import org.springframework.stereotype.Component

@Component
class CustomItemStreamWriter : ItemStreamWriter<Long> {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    override fun open(executionContext: ExecutionContext) {
        log.info("itemWriterStream call open")
    }

    override fun update(executionContext: ExecutionContext) {
        log.info("itemWriterStream call update")
    }

    override fun close() {
        log.info("itemWriterStream call close")
    }

    override fun write(items: MutableList<out Long>) {
        log.info("writer receive items: $items")
        items.forEach { item -> log.info("write: $item") }
    }
}