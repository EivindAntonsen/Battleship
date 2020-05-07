create table if not exists battleship.player_ship
(
    id           serial primary key,
    player_id    int not null,
    ship_type_id int not null,
    foreign key (player_id) references battleship.player (id),
    foreign key (ship_type_id) references battleship.ship_type (id)
);
