package no.esa.battleship.game

data class Player(val id: Int,
                  val gameId: Int) : IPlayer {

    override fun receiveShot(coordinate: Coordinate): Boolean {
        TODO()
    }

    override fun shoot(): Coordinate {
        TODO()
    }
}
