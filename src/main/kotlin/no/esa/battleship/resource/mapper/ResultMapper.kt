package no.esa.battleship.resource.mapper

import no.esa.battleship.model.ResultDTO
import no.esa.battleship.service.domain.Result

object ResultMapper {
    fun toDto(result: Result): ResultDTO = ResultDTO(result.gameId, result.winningPlayerId)
}
