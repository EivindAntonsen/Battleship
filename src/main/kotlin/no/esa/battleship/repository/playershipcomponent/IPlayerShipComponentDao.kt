package no.esa.battleship.repository.playershipcomponent

interface IPlayerShipComponentDao {
    fun findAllComponents(shipId: Int)
}
