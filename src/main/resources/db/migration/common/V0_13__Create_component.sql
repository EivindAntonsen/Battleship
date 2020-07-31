create table if not exists battleship.component
(
    id            serial primary key,
    ship_id       int     not null,
    coordinate_id int     not null,
    is_destroyed  boolean not null,
    foreign key (ship_id) references battleship.ship (id) on delete cascade,
    foreign key (coordinate_id) references battleship.coordinate (id) on delete cascade
);
