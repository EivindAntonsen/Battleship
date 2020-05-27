package no.esa.battleship.repository.entity

import java.time.LocalDateTime
import java.util.*

data class GameEntity(val id: Int,
                      val dateTime: LocalDateTime,
                      val gameSeriesId: UUID?,
                      val isConcluded: Boolean)
