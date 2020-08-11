package no.esa.battleship.service.domain

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.ComponentAlignmentException
import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.utils.isAdjacentWith
import no.esa.battleship.utils.validateElements

data class Components(private val shipType: ShipType,
                      private val components: List<ComponentEntity>) : Iterable<ComponentEntity> {

    init {
        fun verifySize(components: List<ComponentEntity>): Boolean {
            return components.size == shipType.size
        }

        fun verifyIntegrity(components: List<ComponentEntity>): Boolean {
            return components.map { it.coordinateEntity }.sortedBy { coordinate ->
                when (getAxis(components)) {
                    HORIZONTAL -> coordinate.horizontalPositionAsInt()
                    VERTICAL -> coordinate.verticalPosition
                }
            }.validateElements { current, next ->
                current isAdjacentWith next
            }
        }

        require(verifySize(components) && verifyIntegrity(components))
    }

    private fun getAxis(components: List<ComponentEntity>): Axis {
        val verticalSpan = components.map {
            it.coordinateEntity.verticalPosition
        }.distinct().size
        val horizontalSpan = components.map {
            it.coordinateEntity.horizontalPositionAsInt()
        }.distinct().size

        return when {
            verticalSpan == 1 && horizontalSpan > 1 -> HORIZONTAL
            verticalSpan > 1 && horizontalSpan == 1 -> VERTICAL
            else -> throw ComponentAlignmentException(horizontalSpan, verticalSpan)
        }
    }

    override fun iterator(): Iterator<ComponentEntity> {
        return components.iterator()
    }

    fun shipTypeId(): Int {
        return shipType.id
    }
}
