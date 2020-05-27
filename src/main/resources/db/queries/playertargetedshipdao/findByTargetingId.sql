select pt.id,
       player_id,
       targeting_mode_id,
       pts.id,
       player_targeting_id,
       player_ship_id
from battleship.player_targeting pt
         join battleship.player_targeted_ship pts
             on pt.id = pts.player_targeting_id
where pt.id = :player_targeting_id;
