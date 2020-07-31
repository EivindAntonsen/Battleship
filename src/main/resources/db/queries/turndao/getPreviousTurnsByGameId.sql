select t.id,
       game_id,
       game_turn,
       player_id,
       target_player_id,
       coordinate_id,
       is_hit,
       c.y_coordinate,
       c.x_coordinate
from battleship.turn t
         join battleship.coordinate c on t.coordinate_id = c.id
where game_id = :game_id;
