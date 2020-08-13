package no.esa.battleship.service.domain

import no.esa.battleship.enums.ShipType
import no.esa.battleship.repository.entity.ComponentEntity

data class Components(private val shipType: ShipType,
                      private val components: List<ComponentEntity>) : Iterable<ComponentEntity> {

    override fun iterator(): Iterator<ComponentEntity> {
        return components.iterator()
    }

    fun shipTypeId(): Int {
        return shipType.id
    }
}
