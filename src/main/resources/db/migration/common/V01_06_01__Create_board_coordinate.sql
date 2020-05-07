create table if not exists battleship.board_coordinate
(
    id           serial primary key,
    x_coordinate char not null,
    y_coordinate int  not null
);
