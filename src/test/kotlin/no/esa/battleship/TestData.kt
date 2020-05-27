package no.esa.battleship

import no.esa.battleship.service.domain.Game
import no.esa.battleship.service.domain.Player
import java.time.LocalDateTime

object TestData {

    // hardcoded test values from db/migration/test
    const val gameId = 1
    const val playerOneId = 1
    const val playerTwoId = 2

    val playersInTestGame = listOf(player(playerOneId, gameId),
                                   player(playerTwoId, gameId))

    fun game(id: Int): Game {
        return Game(id,
                    LocalDateTime.of(2020,
                                     1,
                                     1,
                                     0,
                                     0),
                    null,
                    false)
    }

    fun player(id: Int = 1, gameId: Int = 1): Player = Player(id, gameId)
}
