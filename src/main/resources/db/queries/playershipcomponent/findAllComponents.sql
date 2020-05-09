select psc.id,
       psc.player_ship_id,
       psc.board_coordinate_id,
       psc.is_destroyed,
       bc.id,
       bc.x_coordinate,
       bc.y_coordinate
from battleship.player_ship_component psc
         JOIN battleship.board_coordinate bc
              on psc.board_coordinate_id = bc.id
where psc.player_ship_id = :player_ship_id;
