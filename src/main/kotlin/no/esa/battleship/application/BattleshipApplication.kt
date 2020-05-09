package no.esa.battleship.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.ComponentScan
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication(scanBasePackages = ["no.esa.battleship"])
@EnableCaching
@EnableSwagger2
@ComponentScan(basePackages = ["no.esa.battleship"])
class BattleshipApplication

fun main(args: Array<String>) {
    runApplication<BattleshipApplication>(*args)
}
