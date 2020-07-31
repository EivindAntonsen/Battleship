create table if not exists battleship.ship_status
(
    id             serial primary key,
    ship_status_id int not null,
    ship_id        int not null,
    foreign key (ship_status_id) references battleship.ship_status_type (id) on delete cascade,
    foreign key (ship_id) references battleship.ship (id) on delete cascade
);
