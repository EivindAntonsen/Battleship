create table battleship.game
(
    id           serial primary key,
    datetime     timestamp not null,
    is_concluded boolean   not null
);
