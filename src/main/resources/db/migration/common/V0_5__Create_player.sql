create table if not exists battleship.player
(
    id             serial primary key,
    player_type_id int not null,
    game_id        int not null,
    foreign key (game_id) references battleship.game (id) on delete cascade,
    foreign key (player_type_id) references battleship.player_type (id) on delete cascade
);
