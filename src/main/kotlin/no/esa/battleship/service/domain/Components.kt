package no.esa.battleship.service.domain

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.ComponentsException.*
import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.utils.isAdjacentWith
import java.util.Optional.empty
import java.util.Optional.of

data class Components(private val shipType: ShipType,
                      private val componentEntities: List<ComponentEntity>) : Iterable<ComponentEntity> {

    init {
        fun verifySize(componentEntities: List<ComponentEntity>) {
            if (componentEntities.size != shipType.size) throw Composition(shipType, componentEntities.size)
        }

        fun verifyIntegrity(componentEntities: List<ComponentEntity>) {
            componentEntities.map { it.coordinateEntity }.sortedBy { coordinate ->
                when (getAxis(componentEntities)) {
                    HORIZONTAL -> coordinate.horizontalPositionAsInt()
                    VERTICAL -> coordinate.vertical_position
                }
            }.fold(empty<CoordinateEntity>()) { acc, coordinate ->
                when {
                    acc.isEmpty -> of(coordinate)
                    acc.isPresent && acc.get() isAdjacentWith coordinate -> of(coordinate)
                    else -> throw IntegrityViolation (acc.get(), coordinate)
                }
            }
        }

        verifySize(componentEntities)
        verifyIntegrity(componentEntities)
    }

    fun getAxis(componentEntities: List<ComponentEntity>): Axis {
        val verticalSpan = componentEntities.map {
            it.coordinateEntity.vertical_position
        }.distinct().size
        val horizontalSpan = componentEntities.map {
            it.coordinateEntity.horizontal_position
        }.distinct().size

        return when {
            verticalSpan == 1 && horizontalSpan > 1 -> HORIZONTAL
            verticalSpan > 1 && horizontalSpan == 1 -> VERTICAL
            else -> throw Alignment(horizontalSpan, verticalSpan)
        }
    }

    override fun iterator(): Iterator<ComponentEntity> {
        return componentEntities.iterator()
    }

    fun shipTypeId(): Int {
        return shipType.id
    }
}
