package no.esa.battleship.aspect

import no.esa.battleship.utils.abbreviate
import no.esa.battleship.utils.executeAndMeasureTimeMillis
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class LogAspect {

    @Around("@annotation(no.esa.battleship.annotation.Logged)")
    fun log(joinPoint: ProceedingJoinPoint): Any {
        val logger = getLogger(joinPoint)
        val args = getArgs(joinPoint)
        val signature = getSignature(joinPoint)

        logger.debug("Call \t\t${signature}\t$args")

        val (result, duration) = executeAndMeasureTimeMillis {
            joinPoint.proceed()
        }

        logger.debug("Response \t${result.toString().abbreviate()} in ${duration}ms.")

        return result
    }

    private fun getLogger(joinPoint: ProceedingJoinPoint): Logger {
        return LoggerFactory.getLogger(joinPoint.signature.declaringTypeName)
    }

    private fun getArgs(joinPoint: ProceedingJoinPoint): String {
        return joinPoint.args
                .toList()
                .toString()
                .abbreviate()
    }

    private fun getSignature(joinPoint: ProceedingJoinPoint): String {
        return joinPoint.signature.toString()
                .replace(" ", "\t")
                .replace("no.esa.battleship.", "")
    }
}
