package no.esa.battleship.service.domain

import no.esa.battleship.repository.entity.ShipEntity

data class ShipWithComponents(val ship: ShipEntity,
                              val components: Components)
