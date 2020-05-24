create table if not exists battleship.player_targeting_mode (
    id serial primary key,
    player_id int not null,
    targeting_mode_id int not null,
    foreign key (player_id) references battleship.player(id) on delete cascade,
    foreign key (targeting_mode_id) references battleship.targeting_mode(id) on delete cascade
);
