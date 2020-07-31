create table if not exists battleship.targeting
(
    id                serial primary key,
    player_id         int not null,
    target_player_id  int not null,
    targeting_mode_id int not null,
    foreign key (targeting_mode_id) references battleship.targeting_mode (id) on delete cascade
);
