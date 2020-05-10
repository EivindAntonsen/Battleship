package no.esa.battleship.enums

import no.esa.battleship.enums.Plane.HORIZONTAL
import no.esa.battleship.enums.Plane.VERTICAL

/**
 * This refers to the direction the ship will face.
 *
 * Irrelevant for the players, but matters for how the ship
 * will be placed during game initialization. The ships are
 * placed with the index component being placed first,
 * and then the next components after wards.
 */
enum class Direction(val plane: Plane) {

    NORTH_SOUTH(VERTICAL),
    EAST_WEST(HORIZONTAL),
    SOUTH_NORTH(VERTICAL),
    WEST_EAST(HORIZONTAL);

    companion object {
        fun random(): Direction {
            return values().toList().shuffled().first()
        }
    }
}
