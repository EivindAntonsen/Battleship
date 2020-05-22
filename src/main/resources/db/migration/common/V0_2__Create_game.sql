create table battleship.game
(
    id             serial primary key,
    datetime       timestamp not null,
    game_series_id uuid,
    is_concluded   boolean   not null
);
