select pt.id,
       game_turn,
       player_id,
       coordinate_id,
       is_hit,
       c.y_coordinate,
       c.x_coordinate
from battleship.player_turn pt
         join battleship.coordinate c on pt.coordinate_id = c.id
where player_id = :player_id;
