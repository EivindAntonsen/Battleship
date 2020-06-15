select c.id,
       c.ship_id,
       c.coordinate_id,
       c.is_destroyed,
       ct.id,
       ct.x_coordinate,
       ct.y_coordinate
from battleship.component c
         JOIN battleship.coordinate ct
              on c.coordinate_id = ct.id
where c.ship_id = :ship_id;
