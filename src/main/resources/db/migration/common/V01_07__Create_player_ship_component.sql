create table if not exists battleship.player_ship_component
(
    id                  serial primary key,
    player_ship_id      int     not null,
    board_coordinate_id int     not null,
    is_destroyed        boolean not null,
    foreign key (player_ship_id) references battleship.player_ship (id),
    foreign key (board_coordinate_id) references battleship.board_coordinate (id)
);
