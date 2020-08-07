docker run --rm -d --name battleship_database -v my_dbdata:/var/lib/postgresql/data -p 54320:5432 -e POSTGRES_PASSWORD=docker postgres:11
