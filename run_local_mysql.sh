docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=springdb -e MYSQL_USER=springuser -e MYSQL_PASSWORD=springpassword -p 3306:3306 -d mysql:8.0
