CREATE DATABASE IF NOT EXISTS moviedb;
USE moviedb;

CREATE TABLE if not exists movies (
    id varchar(10) primary key NOT NULL DEFAULT '',
    title varchar(100) NOT NULL DEFAULT '',
    year INT NOT NULL ,
    director varchar(100) NOT NULL DEFAULT '',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

-- USE moviedb;
-- ALTER TABLE movies ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0.00;
-- UPDATE movies SET price = ROUND(5 + (RAND() * 20), 2);

CREATE TABLE IF NOT EXISTS stars (
    id varchar(10) primary key NOT NULL DEFAULT '',
    name varchar(100) NOT NULL DEFAULT '',
    birth_year INT DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS stars_in_movies (
    star_id varchar(10) NOT NULL,
    movie_id varchar(10) NOT NULL,
    FOREIGN KEY (star_id) REFERENCES stars(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS genres (
    id INT AUTO_INCREMENT primary key NOT NULL,
    name varchar(32) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS genres_in_movies (
    genre_id INT NOT NULL,
    movie_id varchar(10) NOT NULL,
    FOREIGN KEY (genre_id) REFERENCES genres(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS credit_cards (
    id varchar(20) PRIMARY KEY NOT NULL DEFAULT '',
    first_name varchar(50) NOT NULL DEFAULT '',
    last_name varchar(50) NOT NULL DEFAULT '',
    expiration date NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    first_name varchar(50) NOT NULL DEFAULT '',
    last_name varchar(50) NOT NULL DEFAULT '',
    credit_card_id varchar(20) NOT NULL,
    address varchar(200) NOT NULL DEFAULT '',
    email varchar(50) NOT NULL DEFAULT '',
    password varchar(20) NOT NULL DEFAULT '',
    FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id)
);

CREATE TABLE IF NOT EXISTS sales (
    id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    customer_id INT NOT NULL,
    movie_id varchar(10) NOT NULL,
    sale_date DATE NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS ratings (
    movie_id varchar(10) NOT NULL,
    rating FLOAT NOT NULL,
    vote_count INT NOT NULL,
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS employees (
    email varchar(50) PRIMARY KEY,
    password varchar(20) NOT NULL,
    fullname varchar(100)
);

INSERT INTO employees VALUES ('classta@email.edu', 'classta', 'TA CS122B');