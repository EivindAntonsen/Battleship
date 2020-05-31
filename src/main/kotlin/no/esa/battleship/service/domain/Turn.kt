package no.esa.battleship.service.domain

data class Turn(val id: Int,
                val gameTurn: Int,
                val playerId: Int,
                val targetPlayerId: Int,
                val coordinate: Coordinate,
                val isHit: Boolean)
