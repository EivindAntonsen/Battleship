create table if not exists battleship.ship_type
(
    id   serial primary key,
    name varchar not null,
    size int     not null
);
