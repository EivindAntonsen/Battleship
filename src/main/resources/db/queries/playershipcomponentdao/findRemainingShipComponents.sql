select psc.id,
       player_ship_id,
       coordinate_id,
       is_destroyed,
       c.id,
       x_coordinate,
       y_coordinate
from battleship.player_ship_component psc
         join battleship.coordinate c on psc.coordinate_id = c.id
where is_destroyed is false
