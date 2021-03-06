create table if not exists battleship.turn
(
    id               serial primary key,
    game_id          int     not null,
    game_turn        int     not null,
    player_id        int     not null,
    target_player_id int     not null,
    coordinate_id    int     not null,
    is_hit           boolean not null,
    foreign key (game_id) references battleship.game (id) on delete cascade,
    foreign key (player_id) references battleship.player (id) on delete cascade,
    foreign key (target_player_id) references battleship.player (id) on delete cascade,
    foreign key (coordinate_id) references battleship.coordinate (id) on delete cascade
);
