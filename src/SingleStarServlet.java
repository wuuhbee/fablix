import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private MongoClient mongoClient;
    private MongoDatabase database;

    public void init(ServletConfig config) {
        try {
            String user = System.getenv("mongouser");
            String pass = System.getenv("mongopass");
            String uri = "mongodb://" + user + ":" + pass + "@localhost:27017/moviedb";
            mongoClient = MongoClients.create(uri);
            database = mongoClient.getDatabase("moviedb");
            System.out.println("MongoDB Connection Pool Initialized!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String id = request.getParameter("id");

        request.getServletContext().log("getting id: " + id);

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> starsCollection = database.getCollection("stars");

            Bson filter = Filters.eq("_id", id);
            Document star = starsCollection.find(filter).first();

            JsonArray moviesArray = new JsonArray();
            MongoCollection<Document> movieCollection = database.getCollection("movies");
            if (star != null) {
                String starName = star.getString("name");
                Integer birthYear = star.getInteger("birth_year");
                List<String> movieIds = star.getList("movies", String.class);

                if (movieIds != null && !movieIds.isEmpty()) {
                    Bson movieFilter = Filters.in("_id", movieIds);
                    FindIterable<Document> movies = movieCollection
                            .find(movieFilter)
                            .sort(Sorts.descending("year"));

                    for (Document movieFound : movies) {
                        JsonObject movie = new JsonObject();
                        movie.addProperty("star_id", id);
                        movie.addProperty("star_name", starName);
                        movie.addProperty("star_dob", birthYear);

                        movie.addProperty("movie_id", movieFound.getString("_id"));
                        movie.addProperty("movie_title", movieFound.getString("title"));
                        movie.addProperty("movie_year", movieFound.getInteger("year"));
                        movie.addProperty("movie_director", movieFound.getString("director"));
                        moviesArray.add(movie);
                    }
                }
            }
            out.write(moviesArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
            out.close();
        }
    }
}
