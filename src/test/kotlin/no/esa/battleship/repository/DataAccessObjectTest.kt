package no.esa.battleship.repository

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.esa.battleship.TestData
import no.esa.battleship.config.TestConfig
import no.esa.battleship.enums.PlayerType
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.Strategy
import no.esa.battleship.repository.component.ComponentDao
import no.esa.battleship.repository.coordinate.CoordinateDao
import no.esa.battleship.repository.game.GameDao
import no.esa.battleship.repository.player.PlayerDao
import no.esa.battleship.repository.playerstrategy.PlayerStrategyDao
import no.esa.battleship.repository.ship.ShipDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.time.LocalDateTime

internal class DataAccessObjectTest {

    private val logger: Logger = mockk()
    private val gameDao = GameDao(TestConfig.jdbcTemplate)
    private val coordinateDao = CoordinateDao(TestConfig.jdbcTemplate)
    private val playerDao = PlayerDao(TestConfig.jdbcTemplate)
    private val playerShipComponentDao = ComponentDao(TestConfig.jdbcTemplate)
    private val playerShipDao = ShipDao(TestConfig.jdbcTemplate)
    private val playerStrategyDao = PlayerStrategyDao(TestConfig.jdbcTemplate)

    @BeforeEach
    fun init() {
        every { logger.info(any()) } just Runs
        every { logger.info(any<String>(), any(), any(), any()) } just Runs
        every { logger.info(any<String>(), any(), any(), any(), any(), any()) } just Runs
    }

    @Nested
    inner class CoordinateDaoTests {
        @Test
        fun `findAll should return a list of 100 coordinates`() {
            assert(coordinateDao.getAll().size == 100)
        }
    }

    @Nested
    inner class GameDaoTests {
        @Test
        fun `save should create a row that can be retrieved by its game id`() {
            val datetime = LocalDateTime.now()
            val gameId = gameDao.save(datetime)
            val game = gameDao.get(gameId)

            assert(gameId == game.id)
        }

        @Test
        fun `conclude should update is_concluded to true`() {
            assert(!gameDao.get(TestData.gameId).isConcluded)

            gameDao.conclude(TestData.gameId)

            assert(gameDao.get(TestData.gameId).isConcluded)
        }

        @Test
        fun `get should retrieve a game` () {
            val game = gameDao.get(TestData.gameId)

            assert(game.dateTime.isBefore(LocalDateTime.now()))
        }
    }

    @Nested
    inner class PlayerDaoTests {
        @Test
        fun `find should find a player that belongs to a game`() {
            val player = playerDao.get(TestData.playerOneId)

            assert(player.gameId == TestData.gameId)
        }

        @Test
        fun `findPlayersInGame should return players sharing a gameId`() {
            val players = playerDao.getPlayersInGame(TestData.gameId)
            val playersGameIdIsIdentical = players.map { it.gameId }.distinct().size == 1

            assert(playersGameIdIsIdentical)
        }

        @Test //invalid game state with 3 players, but whatever
        fun `save should save a new player to an existing game`() {
            val playerId = playerDao.save(TestData.gameId, PlayerType.AI.id)

            assert(playerDao.get(playerId).gameId == TestData.gameId)
        }
    }

    @Nested
    inner class ComponentDaoTests {
        @Test
        fun `findByPlayerShipId should return a list of components`() {
            val components = playerShipComponentDao.getByShipId(1)
            val componentsSharePlayerShipId = components.map {
                it.shipId
            }.distinct().size == 1

            assert(components.isNotEmpty() && componentsSharePlayerShipId)
        }

        @Test
        fun `update should update the IS_DESTROYED column of a row`() {
            val update = playerShipComponentDao.update(1, true)

            assert(update == 1)
        }

        @Test
        fun `findByGameId should return all components who share game id that aren't destroyed`() {
            val components = playerShipComponentDao.getByGameId(TestData.gameId)

            assert(components.size == 32)
        }
    }

    @Nested
    inner class ShipDaoTests {
        @Test
        fun `find should retrieve a ship by its id`() {
            val ship = playerShipDao.get(1)

            assert(ship.playerId == 1)
        }

        @Test
        fun `findAllShipsForPlayer should return a list of all ships sharing playerId`() {
            val ships = playerShipDao.getAllShipsForPlayer(1)

            assert(ships.size == 5)
        }

        @Test
        fun `save should create a new row`() {
            val ship = playerShipDao.save(1, ShipType.CARRIER.id)

            playerShipDao.get(ship.id)
        }
    }

    @Nested
    inner class PlayerStrategyDaoTests {
        @Test
        fun `find should retrieve an existing player strategy entry`() {
            val strategy = playerStrategyDao.get(TestData.playerOneId)

            assert(strategy == Strategy.RANDOMIZER)
        }
    }
}
