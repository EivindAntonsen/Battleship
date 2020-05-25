package no.esa.battleship.resource.targeting

import no.esa.battleship.service.targeting.TargetingService
import org.slf4j.Logger
import org.springframework.web.bind.annotation.RestController

@RestController
class TargetingController(private val logger: Logger,
                          private val targetingService: TargetingService) {

    fun target() {

    }
}
