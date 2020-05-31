package no.esa.battleship.service.domain

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.ComponentsException.*
import no.esa.battleship.utils.isAdjacentWith

data class Components(private val shipType: ShipType,
                      private val components: List<Component>) : Iterable<Component> {

    init {

        fun verifySize(components: List<Component>) {
            if (components.size != shipType.size) throw Composition(shipType, components.size)
        }

        fun verifyIntegrity(components: List<Component>) {
            components.map { it.coordinate }.sortedBy { coordinate ->
                when (getAxis(components)) {
                    HORIZONTAL -> coordinate.xAsInt()
                    VERTICAL -> coordinate.y
                }
            }.reduce { acc, coordinate ->
                if (acc isAdjacentWith coordinate) coordinate
                else throw IntegrityViolation(acc, coordinate)
            }
        }

        verifySize(components)
        verifyIntegrity(components)
    }

    fun getAxis(components: List<Component>): Axis {
        val verticalSpan = components.map {
            it.coordinate.y
        }.distinct().size
        val horizontalSpan = components.map {
            it.coordinate.xAsInt()
        }.distinct().size

        return when {
            verticalSpan == 1 && horizontalSpan > 1 -> HORIZONTAL
            verticalSpan > 1 && horizontalSpan == 1 -> VERTICAL
            else -> throw Alignment(horizontalSpan, verticalSpan)
        }
    }

    override fun iterator(): Iterator<Component> {
        return components.iterator()
    }

    fun shipTypeId(): Int {
        return shipType.id
    }
}
