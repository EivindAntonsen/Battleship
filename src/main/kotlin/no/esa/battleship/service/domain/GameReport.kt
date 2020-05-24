package no.esa.battleship.service.domain

import no.esa.battleship.enums.Strategy

data class GameReport(val game: Game,
                      val playerInfos: List<PlayerInfo>,
                      val result: Result)
