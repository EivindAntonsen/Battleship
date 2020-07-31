select t.id, player_id, targeting_mode_id, ts.id, targeting_id, ship_id
from battleship.targeting t
         join battleship.targeted_ship ts on t.id = ts.targeting_id
where t.id = :targeting_id;
