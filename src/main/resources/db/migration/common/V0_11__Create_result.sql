create table if not exists battleship.result
(
    id                serial primary key,
    game_id           int not null,
    winning_player_id int,
    foreign key (game_id) references battleship.game (id),
    foreign key (winning_player_id) references battleship.player (id)
)
