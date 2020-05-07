create table if not exists battleship.strategy
(
    id          serial primary key,
    name varchar not null,
    description varchar not null
);
