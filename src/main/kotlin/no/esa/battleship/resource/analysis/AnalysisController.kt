package no.esa.battleship.resource.analysis

import no.esa.battleship.api.AnalysisApi
import no.esa.battleship.service.analysis.IAnalysisService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class AnalysisController(private val analysisService: IAnalysisService) : AnalysisApi {

    override fun analyseGames(amount: Int): ResponseEntity<UUID> {

        require(amount in 5..25) { "Invalid amount of games... Should be between 5-25!" }

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(analysisService.analyseGames(amount))
    }
}
