create table if not exists battleship.player
(
    id      serial primary key,
    game_id int not null,
    foreign key (game_id) references battleship.game (id)
);
