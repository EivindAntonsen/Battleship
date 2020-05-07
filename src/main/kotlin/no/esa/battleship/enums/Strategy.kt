package no.esa.battleship.enums

/**
 * Strategies for where to position the ships as well
 * as where to shoot for the enemy ships.
 *
 * @property RANDOMIZER is fully random coordinate selection.
 * @property MATHEMATICIAN attempts to optimize for greatest chance.
 * @property MIMIC simply mimics the opponent.
 */
enum class Strategy(val id: Int) {
    RANDOMIZER(1),
    MATHEMATICIAN(2),
    MIMIC(3)
}
