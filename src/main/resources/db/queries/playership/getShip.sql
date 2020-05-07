select ps.id,
       player_id,
       ship_type_id,
       psc.id,
       player_ship_id,
       board_coordinate_id,
       is_destroyed
from battleship.player_ship ps
         join battleship.player_ship_component psc
              on ps.id = psc.player_ship_id
                  and ps.id = :id;
