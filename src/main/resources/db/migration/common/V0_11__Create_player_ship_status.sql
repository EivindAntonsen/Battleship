create table if not exists battleship.player_ship_status (
    id serial primary key,
    ship_status_id int not null,
    player_ship_id int not null,
    foreign key (ship_status_id) references battleship.ship_status(id) on delete cascade,
    foreign key (player_ship_id) references battleship.player_ship(id) on delete cascade
);
