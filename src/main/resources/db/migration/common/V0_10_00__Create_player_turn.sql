create table if not exists battleship.player_turn
(
    id                serial primary key,
    game_turn         int     not null,
    player_id         int     not null,
    coordinate_id     int     not null,
    shot_direction_id int     not null,
    is_hit            boolean not null,
    foreign key (player_id) references battleship.player (id),
    foreign key (coordinate_id) references battleship.coordinate (id),
    foreign key (shot_direction_id) references battleship.shot_direction (id)
);
