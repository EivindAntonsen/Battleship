# Battleship

Battleship is a two-player strategy type guessing game. This project has built two AIs battling eachother with a containerized data storage for game results/history.

## Installation

Make sure you have Docker installed (https://www.docker.com), then run the script in /docker/database to start the database container. 

env.properties file (placed in `src/main/resources/properties`):
````properties
database.schema=battleship
database.url=jdbc:postgresql://localhost:54320/postgres
database.username=postgres
database.password=docker
````


### Usage

Currently this game has no frontend. It may come at some point. Until then, games are played through Swagger (localhost:8098/swagger-ui.html).

### So what's this project for really?

I use this project to show my code style and how I solve certain problems (database integrations, aspect oriented logging etc). It is not meant to display a performance-optimized Battleship game, but rather a look into how I code.

### Coming features

Human vs AI games, simple frontend are the two planned features next up.
