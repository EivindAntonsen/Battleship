select ss.id, ship_status_id, ship_id, s.id, player_id, ship_type_id
from battleship.ship_status ss
         join battleship.ship s on ss.ship_id = s.id
         join battleship.player p on s.player_id = p.id
where p.id = :player_id;
