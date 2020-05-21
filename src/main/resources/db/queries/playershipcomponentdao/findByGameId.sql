select psc.id,
       player_ship_id,
       coordinate_id,
       is_destroyed,
       c.x_coordinate,
       c.y_coordinate
from battleship.player_ship_component psc
         join battleship.coordinate c on psc.coordinate_id = c.id
         join battleship.player_ship ps on psc.player_ship_id = ps.id
         join battleship.player p on ps.player_id = p.id
where is_destroyed is false
  and p.game_id = :game_id;
