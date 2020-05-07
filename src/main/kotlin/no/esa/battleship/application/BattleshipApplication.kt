package no.esa.battleship.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(scanBasePackages = ["no.esa.battleship"])
@EnableCaching
class BattleshipApplication

fun main(args: Array<String>) {
    runApplication<BattleshipApplication>(*args)
}
