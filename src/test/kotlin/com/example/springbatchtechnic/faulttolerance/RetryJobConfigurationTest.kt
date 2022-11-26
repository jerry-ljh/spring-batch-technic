package com.example.springbatchtechnic.faulttolerance

import com.example.springbatchtechnic.BatchTestConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["spring.batch.job.names=retryJob"])
class RetryJobConfigurationTest : BatchTestConfig() {

    @Test
    fun `run retryJob`() {
        // given
        val jobParameters = JobParametersBuilder()
            .addString("version", System.currentTimeMillis().toString())
            .toJobParameters()

        // when
        val jobExecution: JobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        // then
        jobExecution.status shouldBe BatchStatus.COMPLETED
    }
}