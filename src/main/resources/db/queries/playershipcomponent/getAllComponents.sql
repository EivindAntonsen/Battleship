select psc.id,
       player_ship_id,
       board_coordinate_id,
       bc.id,
       x_coordinate,
       y_coordinate
from battleship.player_ship_component psc
         JOIN battleship.board_coordinate bc
              on psc.board_coordinate_id = bc.id
where player_ship_id = :playerId;
