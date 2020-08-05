select coordinate.*
from battleship.component
         JOIN battleship.coordinate on component.coordinate_id = coordinate.id
         join battleship.ship on component.ship_id = ship.id
where ship.player_id = :player_id