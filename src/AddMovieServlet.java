import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddMovieServlet", urlPatterns = "/_dashboard/api/add-movie")
public class AddMovieServlet extends HttpServlet {

    private MongoClient mongoClient;
    private MongoDatabase database;

    public void init(ServletConfig config) {
        try {
            String user = System.getenv("mongouser");
            String pass = System.getenv("mongopass");
            String uri = "mongodb://" + user + ":" + pass + "@localhost:27017/moviedb";
            mongoClient = MongoClients.create(uri);
            database = mongoClient.getDatabase("moviedb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String genre = request.getParameter("genre");
        String starName = request.getParameter("starName");
        System.out.println("title=" + title + " year=" + year + " director=" + director + " genre=" + genre + " starName=" + starName);

        try (PrintWriter out = response.getWriter()) {

            MongoCollection<Document> movieCollection = database.getCollection("movies");
            MongoCollection<Document> genreCollection = database.getCollection("genres");
            MongoCollection<Document> starCollection = database.getCollection("stars");

            StringBuilder messages = new StringBuilder();

            //if exist
            Document existingMovie = movieCollection.find(
                    Filters.and(
                            Filters.eq("title", title),
                            Filters.eq("year", Integer.parseInt(year)),
                            Filters.eq("director", director)
                    )
            ).first();

            if (existingMovie != null) {
                messages.append("Movie ").append(title).append(" (").append(year).append(") already exists. No change made.");
                JsonObject returnMsg = new JsonObject();
                returnMsg.addProperty("status", "fail");
                returnMsg.addProperty("message", messages.toString().trim());
                out.write(returnMsg.toString());
                return;
            }

            // max tt* id and increment, 0 pad to 7 digits
            Document maxMovieDoc = movieCollection.find(
                            Filters.regex("_id", "^tt[0-9]+$"))
                    .sort(Sorts.descending("_id"))
                    .first();

            int newMovieInt = 1;
            if (maxMovieDoc != null) {
                newMovieInt = Integer.parseInt(maxMovieDoc.getString("_id").substring(2)) + 1;
            }
            String movieId = String.format("tt%07d", newMovieInt);

            // insert movie
            List<String> genres = new ArrayList<>();
            genres.add(genre);

            // insert genre if not exist
            Document existingGenre = genreCollection.find(Filters.eq("name", genre)).first();
            if (existingGenre == null) {
                genreCollection.insertOne(new Document("name", genre));
                messages.append("Genre created: ").append(genre).append("\n");
            } else {
                messages.append("Genre exists: ").append(genre).append("\n");
            }

            Document existingStar = starCollection.find(Filters.eq("name", starName)).first();
            String starId;
            if (existingStar == null) {
                Document maxStarDoc = starCollection.find(
                                Filters.regex("_id", "^nm[0-9]+$"))
                        .sort(Sorts.descending("_id"))
                        .first();

                int newStarInt = 1;
                if (maxStarDoc != null) {
                    newStarInt = Integer.parseInt(maxStarDoc.getString("_id").substring(2)) + 1;
                }
                starId = String.format("nm%07d", newStarInt);

                starCollection.insertOne(new Document("_id", starId)
                        .append("name", starName)
                        .append("birth_year", null));
                messages.append("Star created: ").append(starName).append(", ID: ").append(starId).append("\n");
            } else {
                starId = existingStar.getString("_id");
                messages.append("Star exists: ").append(starName).append(", ID: ").append(starId).append("\n");
            }

            List<Document> stars = new ArrayList<>();
            stars.add(new Document("id", starId).append("name", starName));

            Document newMovie = new Document("_id", movieId)
                    .append("title", title)
                    .append("year", Integer.parseInt(year))
                    .append("director", director)
                    .append("genres", genres)
                    .append("stars", stars)
                    .append("rating", 0.0)
                    .append("price", 0.0);

            movieCollection.insertOne(newMovie);
            messages.append("Movie added: ").append(title).append(", ID: ").append(movieId).append("\n");
            messages.append("Genre linked: ").append(genre).append(" to movie ").append(title).append("\n");
            messages.append("Star linked: ").append(starName).append(" to movie ").append(title).append("\n");
            messages.append("Movie add successful");

            JsonObject returnMsg = new JsonObject();
            returnMsg.addProperty("status", "success");
            returnMsg.addProperty("message", messages.toString().trim());
            out.write(returnMsg.toString());

        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error", e);

            response.setStatus(500);
            out.close();
        }
    }
}