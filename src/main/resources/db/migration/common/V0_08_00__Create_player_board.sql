create table if not exists battleship.player_board
(
    id        serial primary key,
    player_id int not null,
    foreign key (player_id) references battleship.player (id)
);
