select player.id, player_type_id, game_id
from battleship.component
         join battleship.ship on component.ship_id = ship.id and component.is_destroyed = false
         join battleship.player on ship.player_id = player.id
         join battleship.game on player.game_id = game.id and game.id = :game_id
group by player.id, player_type_id, game_id
