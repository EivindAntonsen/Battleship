package no.esa.battleship.repository.mapper

import no.esa.battleship.repository.entity.TurnEntity
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Turn

object TurnMapper {

    fun toTurn(turnEntity: TurnEntity): Turn {
        return with(turnEntity) {
            Turn(id,
                 gameTurn,
                 playerId,
                 targetPlayerId,
                 Coordinate(coordinateEntity.horizontal_position,
                            coordinateEntity.vertical_position),
                 isHit)
        }
    }
}
