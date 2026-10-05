import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

// Declaring a WebServlet called MovieListServlet, which maps to url "/api/movies"
@WebServlet(name = "MovieListServlet", urlPatterns = "/api/movies")
public class MovieListServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource
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

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();


        // Pagination updates, get page and limit from URL in HTTP request
        int page = 1;
        int limit = 20;

        String pageUrl = request.getParameter("page");
        String limitUrl = request.getParameter("limit");

        if (pageUrl != null) { page = Integer.parseInt(pageUrl); }
        if (limitUrl != null) { limit = Integer.parseInt(limitUrl); }

        int offset = (page - 1) * limit; // skip num pages
        // page 2: skip (2-1) * N results per page

        try {
            MongoCollection<Document> movieCollection = database.getCollection("movies");

            // top 20 movies sorted by rating desc with pagination
            FindIterable<Document> movies = movieCollection.find()
                    .sort(Sorts.descending("rating"))
                    .skip(offset)
                    .limit(limit);

            JsonArray movieJsonArray = new JsonArray();

            // Iterate through each row of rs
            for (Document movie : movies) {
                String movieId = movie.getString("_id");
                String title = movie.getString("title");
                Integer year = movie.getInteger("year");
                String director = movie.getString("director");
                Double rating = movie.getDouble("rating") != null ? movie.getDouble("rating") : 0.0;

                JsonArray genreArray = new JsonArray();
                List<String> mongoGenres = movie.getList("genres", String.class);
                if (mongoGenres != null) {
                    int genreCount = 0;
                    for (String genre : mongoGenres) {
                        if (genreCount >= 3) break;
                        genreArray.add(genre);
                        genreCount++;
                    }
                }

                JsonArray starArray = new JsonArray();
                List<Document> mongoStars = movie.getList("stars", Document.class);
                if (mongoStars != null) {
                    int starCount = 0;
                    for (Document starDoc : mongoStars) {
                        if (starCount >= 3) break;
                        JsonObject starObj = new JsonObject();
                        starObj.addProperty("id", starDoc.getString("id"));
                        starObj.addProperty("name", starDoc.getString("name"));
                        starArray.add(starObj);
                        starCount++;
                    }
                }

                // Create a JsonObject based on the data we retrieve from rs

                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("id", movieId);
                movieJson.addProperty("title", title);
                movieJson.addProperty("year", year);
                movieJson.addProperty("director", director);
                movieJson.add("genres", genreArray);
                movieJson.add("stars", starArray);
                movieJson.addProperty("rating", rating);

                movieJsonArray.add(movieJson);
            }

            // Write JSON string to output
            out.write(movieJsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
        // Always remember to close db connection after usage. Here it's done by try-with-resources
    }
}