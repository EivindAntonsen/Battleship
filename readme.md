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

#### Some concepts used in this project

* Aspect oriented programming
    * Logging
        * Repetitive call/response log statements happen in `/battleship/aspect/LogAspect.kt`.
        * Debug by default, as these calls should be uninteresting in most cases.
    * Error handling
        * Repetitive try/catch statements with subsequent logging happen in `/battleship/aspect/DataAccessAspect.kt`.
        * Calls to data access objects are similar by default, and handle errors the same way. This has been moved from the
          actual dao-implementations to an aspect in favor of more readable code in the dao.
* Controller advice
    * Exception handling
        * Exceptions should not bubble all the way up to the frontend. There is an exception handler
          in `/battleship/resource/exceptions/ExceptionHandler.kt` that maps exceptions to more appropriate user-friendly error messages in the resource package.
* OpenAPI specification
    * The game API is automatically generated from code that follows the OpenAPI specification.
      This is found in `src/main/resources/api-definition`. 
    * This means the Battleship API will be language agnostic and can be integrated with anything following the same standard.
* Extension functions
    * `Iterable<T>.validateElements` in `/battleship/utils/Utils.kt`.
    * `Iterable<T>.flatMapIndexedNotNull` in `/battleship/utils/Utils.kt`.
