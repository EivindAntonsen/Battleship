create table if not exists battleship.player_board_history
(
    id                  serial primary key,
    player_board_id     int     not null,
    board_coordinate_id int     not null,
    shot_type_id        int     not null,
    is_hit              boolean not null,
    foreign key (player_board_id) references battleship.player_board (id),
    foreign key (board_coordinate_id) references battleship.board_coordinate (id),
    foreign key (shot_type_id) references battleship.shot_direction (id)
);
