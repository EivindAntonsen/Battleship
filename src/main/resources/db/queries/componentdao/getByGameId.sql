select c.id, ship_id, coordinate_id, is_destroyed, ct.x_coordinate, ct.y_coordinate
from battleship.component c
         join battleship.coordinate ct on c.coordinate_id = ct.id
         join battleship.ship ps on c.ship_id = ps.id
         join battleship.player p on ps.player_id = p.id
where is_destroyed is false
  and p.game_id = :game_id;
