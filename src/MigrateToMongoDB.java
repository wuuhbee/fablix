import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MigrateToMongoDB {

    private String uri;
    private MongoClient mongoClient;
    private MongoDatabase myNewDB;

    public static void main(String[] args) throws SQLException {
        MigrateToMongoDB migrator = new MigrateToMongoDB();
        migrator.migrateMovies();
        migrator.migrateStars();
        migrator.migrateCustomerAndCreditCard();
        migrator.migrateSalesData();
        migrator.migrateCCData();
        migrator.migrateEmployeeData();
    }

    public MigrateToMongoDB() {
        uri = returnMongoURI();
        try {
            mongoClient = MongoClients.create(uri);
            myNewDB = mongoClient.getDatabase("moviedb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String returnMongoURI() {
        String user = System.getenv("mongouser");
        String pass = System.getenv("mongopass");
        return  "mongodb://" + user + ":" + pass + "@localhost:27017/moviedb";
    }

    private void insertIntoMongo(String collectionName, List<Document> insertBuffer) {
        myNewDB.createCollection(collectionName);
        MongoCollection<Document> collection = myNewDB.getCollection(collectionName);
        if (insertBuffer != null) {
            collection.insertMany(insertBuffer);
        } else {
            System.out.println("Nothing inserted to Mongo.");
        }
    }

    private void migrateMovies() {
        List<Document> allMovies = readMoviesFromMySQL();
        insertIntoMongo("movies", allMovies);
    }

    private List<Document> readMoviesFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String moviesQuery = """
                SELECT m.id as _id,
                       m.title,
                       m.year,
                       m.director,
                       m.price,
                       r.rating,
                       r.vote_count
                FROM movies m
                LEFT JOIN ratings r ON m.id = r.movie_id""";

        String genresQuery = """
                SELECT g.name
                FROM genres_in_movies gim
                JOIN genres g ON gim.genre_id = g.id
                WHERE gim.movie_id = ?
                ORDER BY g.name""";

        String starsQuery = """
                SELECT s.id, s.name, s.birth_year, COUNT(sim2.movie_id) as movie_count
                FROM stars_in_movies sim
                JOIN stars s ON sim.star_id = s.id
                LEFT JOIN stars_in_movies sim2 ON sim2.star_id = s.id
                WHERE sim.movie_id = ?
                GROUP BY s.id, s.name, s.birth_year
                ORDER BY movie_count DESC, s.name ASC""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement statement = conn.createStatement();
             ResultSet movieResults = statement.executeQuery(moviesQuery)) {

            List<Document> insertBuffer = new ArrayList<>();
            System.out.println("Migrating movies");

            PreparedStatement genreStmt = conn.prepareStatement(genresQuery);
            PreparedStatement starStmt = conn.prepareStatement(starsQuery);

            int count = 0;
            while (movieResults.next()) {
                String movieId = movieResults.getString("_id");

                //genre get
                genreStmt.setString(1, movieId);
                ResultSet genreResults = genreStmt.executeQuery();
                List<String> genres = new ArrayList<>();
                while (genreResults.next()) {
                    genres.add(genreResults.getString("name"));
                }
                genreResults.close();

                //stars
                starStmt.setString(1, movieId);
                ResultSet starResults = starStmt.executeQuery();
                List<Document> stars = new ArrayList<>();
                while (starResults.next()) {
                    Document star = new Document("id", starResults.getString("id"))
                            .append("name", starResults.getString("name"))
                            .append("birth_year", starResults.getObject("birth_year"));
                    stars.add(star);
                }
                starResults.close();

                Document movie = new Document("_id", movieId)
                        .append("title", movieResults.getString("title"))
                        .append("year", movieResults.getInt("year"))
                        .append("director", movieResults.getString("director"))
                        .append("price", movieResults.getDouble("price"))
                        .append("rating", movieResults.getObject("rating"))
                        .append("vote_count", movieResults.getObject("vote_count"))
                        .append("genres", genres)
                        .append("stars", stars);

                insertBuffer.add(movie);
                count++;
                if (count % 100 == 0) {
                    System.out.println("Movies processed: " + count);
                }
            }

            genreStmt.close();
            starStmt.close();

            System.out.println("Total movies migrated: " + count);
            return insertBuffer;

        } catch (Exception e) {
            System.out.println("SQL Exception in readMoviesFromMySQL");
            e.printStackTrace();
        }
        return null;
    }

    private void migrateStars() {
        List<Document> allStars = readStarsFromMySQL();
        insertIntoMongo("stars", allStars);
    }

    private List<Document> readStarsFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String starsQuery = """
                SELECT id as _id, name, birth_year
                FROM stars""";

        String moviesQuery = """
                SELECT m.id
                FROM stars_in_movies sim
                JOIN movies m ON sim.movie_id = m.id
                WHERE sim.star_id = ?""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement statement = conn.createStatement();
             ResultSet starResults = statement.executeQuery(starsQuery)) {

            List<Document> insertBuffer = new ArrayList<>();
            System.out.println("Migrating stars...");

            PreparedStatement movieStmt = conn.prepareStatement(moviesQuery);

            int count = 0;
            while (starResults.next()) {
                String starId = starResults.getString("_id");

                movieStmt.setString(1, starId);
                ResultSet movieResults = movieStmt.executeQuery();
                List<String> movies = new ArrayList<>();
                while (movieResults.next()) {
                    movies.add(movieResults.getString("id"));
                }
                movieResults.close();

                Document star = new Document("_id", starId)
                        .append("name", starResults.getString("name"))
                        .append("birth_year", starResults.getObject("birth_year")) //nullable obj not int
                        .append("movies", movies);

                insertBuffer.add(star);
                count++;
                if (count % 100 == 0) {
                    System.out.println("Stars processed: " + count);
                }
            }

            movieStmt.close();

            System.out.println("Total stars migrated: " + count);
            return insertBuffer;

        } catch (Exception e) {
            System.out.println("SQL Exception in readStarsFromMySQL");
            e.printStackTrace();
        }
        return null;
    }

    private void migrateCustomerAndCreditCard() {
        List<Document> allCustomers = readCustomerInfoFromMySQL();
        insertIntoMongo("customers", allCustomers);
    }

    private List<Document> readCustomerInfoFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String customerAndCCInfo = """
                SELECT c.id as _id,
                       c.first_name,
                       c.last_name,
                       c.address,
                       c.email,
                       c.password,
                       cc.id as credit_card_id,
                       cc.expiration,
                       cc.first_name as cc_first_name,
                       cc.last_name as cc_last_name
                FROM customers c
                LEFT JOIN credit_cards cc ON c.credit_card_id = cc.id""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
            Statement statement = conn.createStatement();
            ResultSet customerResults = statement.executeQuery(customerAndCCInfo)) {
            List<Document> insertBuffer = new ArrayList<>();
            System.out.println("Executed Query");
            while (customerResults.next()) {
                Document credit_card = new Document("id", customerResults.getString("credit_card_id"))
                        .append("first_name", customerResults.getString("cc_first_name"))
                        .append("last_name", customerResults.getString("cc_last_name"))
                        .append("expiration", customerResults.getString("expiration"));

                Document customerInfo = new Document("_id", customerResults.getInt("_id"))
                        .append("first_name", customerResults.getString("first_name"))
                        .append("last_name", customerResults.getString("last_name"))
                        .append("address", customerResults.getString("address"))
                        .append("email", customerResults.getString("email"))
                        .append("password", customerResults.getString("password"))
                        .append("credit_cards", credit_card);
                insertBuffer.add(customerInfo);
            }
            if (!insertBuffer.isEmpty()){
                return insertBuffer;
            }
            System.out.println("Result set was empty");
            return null;
        } catch (Exception e) {
            System.out.println("SQL Exception in Customer Info");
            e.printStackTrace();
        }
        return null;
    }

    private void migrateSalesData() {
        List<Document> allSales = readSalesDataFromMySQL();
        insertIntoMongo("sales", allSales);
    }

    private List<Document> readSalesDataFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String sales_info_str = """
                SELECT
                    id,
                    customer_id,
                    movie_id,
                    sale_date
                FROM sales;""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement statement = conn.createStatement();
             ResultSet salesTable = statement.executeQuery(sales_info_str)) {
            List<Document> allSales = new ArrayList<>();
            while (salesTable.next()) {
                Document sale = new Document("_id", salesTable.getInt("id"))
                        .append("customer_id", salesTable.getInt("customer_id"))
                        .append("movie_id", salesTable.getString("movie_id"))
                        .append("sale_date", salesTable.getString("sale_date"))
                        .append("quantity", 1);
                allSales.add(sale);
            }
            if (!allSales.isEmpty()) {
                return allSales;
            }
            return null;
        } catch (Exception e) {
            System.out.println("SQL Exception in Sales");
            e.printStackTrace();
        }
        return null;
    }

    private void migrateCCData() {
        List<Document> allCreditCards = readCCDataFromMySQL();
        insertIntoMongo("creditCards", allCreditCards);
    }

    private List<Document> readCCDataFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String sales_info_str = """
                SELECT
                    id as _id,
                    first_name,
                    last_name,
                    expiration
                FROM credit_cards;""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement statement = conn.createStatement();
             ResultSet ccTable = statement.executeQuery(sales_info_str)) {
            List<Document> allCreditCards = new ArrayList<>();
            while (ccTable.next()) {
                Document sale = new Document("_id", ccTable.getString("_id"))
                        .append("first_name", ccTable.getString("first_name"))
                        .append("last_name", ccTable.getString("last_name"))
                        .append("sale_date", ccTable.getString("expiration"));
                allCreditCards.add(sale);
            }
            if (!allCreditCards.isEmpty()) {
                return allCreditCards;
            }
            return null;
        } catch (Exception e) {
            System.out.println("SQL Exception in Credit Cards");
            e.printStackTrace();
        }
        return null;
    }

    private void migrateEmployeeData() {
        List<Document> allEmployees = readEmployeesFromMySQL();
        insertIntoMongo("employees", allEmployees);
    }

    private List<Document> readEmployeesFromMySQL() {
        String url = System.getenv("url");
        String user = System.getenv("user");
        String pass = System.getenv("pass");

        String sales_info_str = """
                SELECT
                    email,
                    fullname,
                    password
                FROM employees;""";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement statement = conn.createStatement();
             ResultSet employeeTable = statement.executeQuery(sales_info_str)) {
            List<Document> allEmployees = new ArrayList<>();
            while (employeeTable.next()) {
                Document sale = new Document("_email", employeeTable.getString("email"))
                        .append("full_name", employeeTable.getString("fullname"))
                        .append("password", employeeTable.getString("password"));
                allEmployees.add(sale);
            }
            if (!allEmployees.isEmpty()) {
                return allEmployees;
            }
            return null;
        } catch (Exception e) {
            System.out.println("SQL Exception in Employees");
            e.printStackTrace();
        }
        return null;
    }

}