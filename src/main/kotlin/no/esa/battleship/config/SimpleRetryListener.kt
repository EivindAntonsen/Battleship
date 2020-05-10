package no.esa.battleship.config

import org.slf4j.LoggerFactory
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener

class SimpleRetryListener : RetryListener {
    private fun <T : Any, E : Throwable?> getNames(callback: RetryCallback<T, E>): Pair<String, String> {
        val functionName = callback::class.java.enclosingMethod.name
        val className = callback::class.java.enclosingClass.simpleName

        return className to functionName
    }

    override fun <T : Any, E : Throwable?> open(context: RetryContext, callback: RetryCallback<T, E>): Boolean {
        val (className, functionName) = getNames(callback)
        val logger = LoggerFactory.getLogger(className)

        logger.info("Starting retryable function ${className}.$functionName.")

        return true
    }

    override fun <T : Any, E : Throwable?> close(context: RetryContext,
                                                 callback: RetryCallback<T, E>,
                                                 throwable: Throwable?) {
        val (className, functionName) = getNames(callback)
        val logger = LoggerFactory.getLogger(className)

        logger.info("Retryable function ${className}.$functionName finished after ${context.retryCount} failed attempts.")
    }

    override fun <T : Any, E : Throwable> onError(context: RetryContext,
                                                  callback: RetryCallback<T, E>,
                                                  throwable: Throwable) {
        val (className, functionName) = getNames(callback)
        val logger = LoggerFactory.getLogger(className)

        logger.debug("Retryable (${context.retryCount}) function ${className}.$functionName failed: ${context.lastThrowable.message}.")
    }
}
