create table if not exists battleship.ship_status_type
(
    id          serial primary key,
    description varchar not null
);
