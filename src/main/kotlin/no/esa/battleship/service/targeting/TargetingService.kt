package no.esa.battleship.service.targeting

import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playershipstatus.IPlayerShipStatusDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playertargetingmode.IPlayerTargetingModeDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import org.springframework.stereotype.Service

@Service
class TargetingService(private val gameDao: IGameDao,
                       private val playerDao: IPlayerDao,
                       private val playerShipDao: IPlayerShipDao,
                       private val playerShipComponentDao: IPlayerShipComponentDao,
                       private val playerShipStatusDao: IPlayerShipStatusDao,
                       private val playerTargetingModeDao: IPlayerTargetingModeDao,
                       private val playerStrategyDao: IPlayerStrategyDao,
                       private val coordinateDao: ICoordinateDao,
                       private val playerTurnDao: IPlayerTurnDao) {

    /**
     * Rank every coordinate according to how probable it is to have a ship of any given size on it.
     */
    fun rankCoordinates(playerId: Int) {

    }
}
