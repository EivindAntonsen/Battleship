package no.esa.battleship.repository.entity

import java.time.LocalDateTime

data class ResultEntity(val id: Int,
                        val gameId: Int,
                        val winningPlayerId: Int?,
                        val dateTime: LocalDateTime)
