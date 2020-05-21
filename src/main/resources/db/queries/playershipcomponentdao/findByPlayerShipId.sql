select psc.id,
       psc.player_ship_id,
       psc.coordinate_id,
       psc.is_destroyed,
       bc.id,
       bc.x_coordinate,
       bc.y_coordinate
from battleship.player_ship_component psc
         JOIN battleship.coordinate bc
              on psc.coordinate_id = bc.id
where psc.player_ship_id = :player_ship_id;
