package no.esa.battleship.service.domain

import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.ResultEntity

data class GameReport(val gameEntity: GameEntity,
                      val playerInfos: List<PlayerInfo>,
                      val resultEntity: ResultEntity)
