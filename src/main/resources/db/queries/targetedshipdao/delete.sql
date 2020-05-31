delete
from battleship.targeted_ship ts
where ts.targeting_id = :targeting_id
  and ts.ship_id = :ship_id;
