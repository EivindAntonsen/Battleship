package no.esa.battleship

import no.esa.battleship.enums.PlayerType
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
import java.time.LocalDateTime

object TestData {

    // hardcoded test values from db/migration/test
    const val gameId = 1
    const val playerOneId = 1
    const val playerTwoId = 2

    val playersInTestGame = listOf(player(playerOneId, gameId),
                                   player(playerTwoId, gameId))

    fun game(id: Int): GameEntity {
        return GameEntity(id,
                          LocalDateTime.of(2020,
                                           1,
                                           1,
                                           0,
                                           0),
                          false)
    }

    private fun player(id: Int = 1, gameId: Int = 1): PlayerEntity = PlayerEntity(
            id,
            PlayerType.AI.id,
            gameId)
}
