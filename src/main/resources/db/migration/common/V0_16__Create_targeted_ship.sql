create table if not exists battleship.targeted_ship
(
    id             serial primary key,
    targeting_id   int not null,
    ship_id int not null,
    foreign key (targeting_id) references battleship.targeting (id) on delete cascade,
    foreign key (ship_id) references battleship.ship (id) on delete cascade
);
