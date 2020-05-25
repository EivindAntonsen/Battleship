select pss.id,
       ship_status_id,
       player_ship_id,
       ps.id,
       player_id,
       ship_type_id
from battleship.player_ship_status pss
         join battleship.player_ship ps on pss.player_ship_id = ps.id
         join battleship.player p on ps.player_id = p.id
where p.id = :player_id;
