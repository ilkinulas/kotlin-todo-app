package net.ilkinulas

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.slf4j.LoggerFactory


class HealthCheckRule(val healthcheck: () -> SuccessOrFailure, val retryCount: Int, val delay: Long) : TestRule {
    val logger = LoggerFactory.getLogger("todo")

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                logger.info("Healthcheck started")
                for (i in (1..retryCount)) {
                    if (healthcheck().succeeded()) {
                        logger.info("Healthcheck pass.")
                        base.evaluate()
                        return
                    }
                    logger.info("Healthcheck failed")
                    Thread.sleep(delay)
                }
                throw RuntimeException("Healthcheck failed")
            }
        }
    }
}

class SuccessOrFailure(private val failMessage: String?) {
    companion object {
        fun success() = SuccessOrFailure(null)
        fun fail(message: String) = SuccessOrFailure(message)
        fun of(isSuccess: Boolean) = if (isSuccess) success() else fail("Failed")
    }

    fun failed() = failMessage != null
    fun succeeded() = failMessage == null
}
