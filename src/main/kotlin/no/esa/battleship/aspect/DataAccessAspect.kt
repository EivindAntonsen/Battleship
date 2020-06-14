package no.esa.battleship.aspect

import no.esa.battleship.repository.exceptions.DataAccessException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

@Aspect
@Component
class DataAccessAspect {

    @Around("@annotation(no.esa.battleship.annotation.DataAccess)")
    fun tryDataAccessCall(joinPoint: ProceedingJoinPoint): Any {
        return try {
            joinPoint.proceed()
        } catch (error: Exception) {
            val kClass = getKClass(joinPoint)
            val kFunction = getKFunction(joinPoint, kClass)

            throw DataAccessException(kClass, kFunction, error)
        }
    }

    private fun getKClass(joinPoint: ProceedingJoinPoint): KClass<*> {
        return joinPoint.signature.declaringType.kotlin
    }

    private fun getKFunction(joinPoint: ProceedingJoinPoint, kClass: KClass<*>): KFunction<*> {
        return kClass.memberFunctions.first { function ->
            function.name == joinPoint.signature.name
        }
    }
}
