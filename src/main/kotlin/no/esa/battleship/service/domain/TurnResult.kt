package no.esa.battleship.service.domain

import no.esa.battleship.repository.entity.CoordinateEntity

data class TurnResult(val coordinateEntity: CoordinateEntity,
                      val isHit: Boolean,
                      val didDestroyShip: Boolean)
