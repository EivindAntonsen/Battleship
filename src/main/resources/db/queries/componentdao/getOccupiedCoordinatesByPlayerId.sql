select coordinate.*
from battleship.component
         JOIN battleship.coordinate on component.coordinate_id = coordinate.id
where player_id = :player_id