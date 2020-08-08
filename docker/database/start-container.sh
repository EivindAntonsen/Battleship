if [ ! "$(docker ps -q -f name=battleship_database)" ]; then
    if [ "$(docker ps -aq -f status=exited -f name=battleship_database)" ]; then
        docker rm battleship_database
    fi
    docker run --rm -d --name battleship_database -v my_dbdata:/var/lib/postgresql/data -p 54320:5432 -e POSTGRES_PASSWORD=docker postgres:11
fi
