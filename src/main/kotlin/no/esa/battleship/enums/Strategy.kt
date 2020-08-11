package no.esa.battleship.enums

/**
 * Strategies for where to position the ships as well
 * as where to shoot for the enemy ships.
 *
 * @property RANDOMIZER is fully random coordinate selection. This is the village drunk
 *           among strategies. Doesn't even attempt to account for recent hits.
 * @property DEFAULT is random, but also accounts for recent hits to follow up
 *           until it believes a ship has been sunk.
 */
enum class Strategy(val id: Int) {

    RANDOMIZER(1),
    DEFAULT(2),
    HUMAN(3);

    companion object {

        fun random(): Strategy = values().random()

        fun fromInt(id: Int): Strategy? {
            return values().firstOrNull {
                it.id == id
            }
        }
    }
}
