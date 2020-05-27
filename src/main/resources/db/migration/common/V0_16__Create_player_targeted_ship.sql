create table if not exists battleship.player_targeted_ship
(
    id                  serial primary key,
    player_targeting_id int not null,
    player_ship_id      int not null,
    foreign key (player_targeting_id) references battleship.player_targeting (id) on delete cascade,
    foreign key (player_ship_id) references battleship.player_ship (id) on delete cascade
);
