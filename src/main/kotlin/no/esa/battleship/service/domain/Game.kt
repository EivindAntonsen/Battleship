package no.esa.battleship.service.domain

import java.time.LocalDateTime

data class Game(val id: Int, val dateTime: LocalDateTime, val isConcluded: Boolean)
