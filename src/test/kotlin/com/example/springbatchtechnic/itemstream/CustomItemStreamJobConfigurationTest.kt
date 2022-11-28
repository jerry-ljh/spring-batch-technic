package com.example.springbatchtechnic.itemstream

import com.example.springbatchtechnic.BatchTestConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["spring.batch.job.names=customItemStreamJob"])
class CustomItemStreamJobConfigurationTest : BatchTestConfig() {

    @Test
    fun `StepExecutionContext에 진행 상태(OFFSET_IDX)가 저장된다`() {
        // given
        val jobParameters = JobParametersBuilder()
            .addString("version", "1")
            .toJobParameters()

        // when
        val jobExecution: JobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        // then
        val customItemStreamStepExecution = jobExecution.stepExecutions.find { it.stepName == "customItemStreamStep" }!!
        customItemStreamStepExecution.status shouldBe BatchStatus.COMPLETED
        customItemStreamStepExecution.executionContext.getInt(CustomItemStreamReader.OFFSET_IDX_NAME) shouldBe 9

        val customItemStreamThrowStepExecution =
            jobExecution.stepExecutions.find { it.stepName == "customItemStreamThrowStep" }!!
        customItemStreamThrowStepExecution.status shouldBe BatchStatus.FAILED
        customItemStreamThrowStepExecution.executionContext.getInt(CustomItemStreamReader.OFFSET_IDX_NAME) shouldBe 5
    }
}