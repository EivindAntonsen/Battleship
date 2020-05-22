package no.esa.battleship.service.analysis

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.esa.battleship.service.gameplay.IGamePlayService
import no.esa.battleship.service.initialization.IGameInitializationService
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext

@Service
class AnalysisService(private val logger: Logger,
                      private val gameInitializationService: IGameInitializationService,
                      private val gamePlayService: IGamePlayService) : IAnalysisService {

    override fun analyseGames(amount: Int): UUID {
        val gameSeriesId = UUID.randomUUID()

        CoroutineScope(EmptyCoroutineContext).launch {
            val games = (1..amount).map {
                gameInitializationService.initializeNewGame(gameSeriesId)
            }

            games.forEach {
                CoroutineScope(coroutineContext).launch {
                    gamePlayService.playGame(it.id)
                }
            }

        }

        return gameSeriesId
    }
}
