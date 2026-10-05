USE moviedb;

DELIMITER //
    CREATE PROCEDURE add_movie (
        IN p_title VARCHAR(100),
        IN p_year INT,
        IN p_director VARCHAR(100),
        IN p_genre VARCHAR(32),
        IN p_star_name VARCHAR(100)
    )
    BEGIN
        DECLARE v_movie_id VARCHAR(10);
        DECLARE v_star_id VARCHAR(10);
        DECLARE v_genre_id INT;
        DECLARE v_new_id_int INT;
        DECLARE v_max_id VARCHAR(10);


        SELECT id INTO v_movie_id
        FROM movies
        WHERE title = p_title AND year = p_year AND director = p_director
        LIMIT 1;

        IF v_movie_id IS NOT NULL THEN
            SELECT CONCAT('Movie ', p_title, ' (', p_year, ') already exists. No change made.') AS message;
        ELSE
            SELECT MAX(id) INTO v_max_id FROM movies WHERE id REGEXP '^tt[0-9]+$';
            IF v_max_id IS NULL THEN
                SET v_new_id_int = 1;
            ELSE
                SET v_new_id_int = CAST(SUBSTRING(v_max_id, 3) AS UNSIGNED) + 1;
            END IF;
            SET v_movie_id = CONCAT('tt', LPAD(v_new_id_int, 7, '0'));

            INSERT INTO movies (id, title, year, director, price)
                VALUES (v_movie_id, p_title, p_year, p_director, 0.00);

            SELECT CONCAT('Movie added: ', p_title, ', ID: ', v_movie_id) AS message;



            SELECT id INTO v_genre_id
            FROM genres
            WHERE name = p_genre
            LIMIT 1;

            IF v_genre_id IS NULL THEN
                INSERT INTO genres (name) VALUES (p_genre);
                SET v_genre_id = LAST_INSERT_ID();
                SELECT CONCAT('Genre created: ', p_genre, ', ID: ', v_genre_id) AS message;
            ELSE
                SELECT CONCAT('Genre exists: ', p_genre, ', ID: ', v_genre_id) AS message;
            END IF;

            INSERT INTO genres_in_movies (genre_id, movie_id)
                VALUES (v_genre_id, v_movie_id);

            SELECT CONCAT('Genre linked: ', p_genre, ' to movie ', p_title) AS message;



            SELECT id INTO v_star_id
            FROM stars
            WHERE name = p_star_name
            LIMIT 1;

            IF v_star_id IS NULL THEN
                SELECT MAX(id) INTO v_max_id FROM stars;
                SET v_new_id_int = CAST(SUBSTRING(v_max_id, 3) AS UNSIGNED) + 1;
                SET v_star_id = CONCAT('nm', LPAD(v_new_id_int, 7, '0'));

                INSERT INTO stars (id, name, birth_year)
                    VALUES (v_star_id, p_star_name, NULL);

                SELECT CONCAT('Star created: ', p_star_name, ', ID: ', v_star_id) AS message;
            ELSE
                SELECT CONCAT('Star exists: ', p_star_name, ', ID: ', v_star_id) AS message;
            END IF;

            INSERT INTO stars_in_movies (star_id, movie_id)
                VALUES (v_star_id, v_movie_id);

            SELECT CONCAT('Star linked: ', p_star_name, ' to movie ', p_title) AS message;

            SELECT 'Movie add successful' AS message;
        END IF;

    END //
DELIMITER ;



DELIMITER //
    CREATE PROCEDURE add_star (
        IN star_name VARCHAR(100),
        IN star_birth_year INT,
        OUT new_star_id VARCHAR(10)
    )
    BEGIN
        DECLARE max_id VARCHAR(10);
        DECLARE id_prefix VARCHAR(2);
        DECLARE numeric_id INT;

        SELECT MAX(id) INTO max_id FROM stars;
        SET id_prefix = SUBSTRING(max_id, 1, 2);
        SET numeric_id = CAST(SUBSTRING(max_id, 3) AS UNSIGNED);

        SET numeric_id = numeric_id + 1;
        SET new_star_id = CONCAT(id_prefix, numeric_id);

        INSERT INTO stars(id, name, birth_year)
            VALUES (new_star_id, star_name, star_birth_year);
    END //
DELIMITER ;