package no.esa.battleship.service.domain

import java.time.LocalDateTime
import java.util.*

data class Game(val id: Int,
                val dateTime: LocalDateTime,
                val gameSeriesId: UUID?,
                val isConcluded: Boolean)
