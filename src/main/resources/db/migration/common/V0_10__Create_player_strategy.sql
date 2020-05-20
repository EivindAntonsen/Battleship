create table if not exists battleship.player_strategy
(
    id          serial primary key,
    player_id   int not null,
    strategy_id int not null,
    foreign key (player_id) references battleship.player (id),
    foreign key (strategy_id) references battleship.strategy (id)
);
