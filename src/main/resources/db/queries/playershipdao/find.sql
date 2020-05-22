select id,
       player_id,
       ship_type_id
from battleship.player_ship
where id = :id;
