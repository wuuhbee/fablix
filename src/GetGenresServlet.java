import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// WebServlet called GetGenresServlet, which maps to /api/genre-browse
@WebServlet(name = "GetGenresServlet", urlPatterns = "/api/get-genres")
public class GetGenresServlet extends HttpServlet {
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> movies = database.getCollection("movies");

            Bson cleanGenresFilter = Filters.and(
                    Filters.exists("genres"),
                    Filters.type("genres", "string"), // Checks that elements inside are strings
                    Filters.ne("genres", null)
            );

            DistinctIterable<String> allGenres = movies.distinct("genres", cleanGenresFilter, String.class);

            List<String> sortedGenres = new ArrayList<>();
            for (String genre : allGenres) {
                sortedGenres.add(genre);
            }
            Collections.sort(sortedGenres);

            JsonArray genreArray = new JsonArray();
            for (String genre : sortedGenres) {
                JsonObject singleGenre = new JsonObject();
                singleGenre.addProperty("name", genre);
                genreArray.add(singleGenre);
            }
            out.write(genreArray.toString());
        } catch (Exception e) {
            // Create Json Obj to write error message to output
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error", e);

            // Update response status to 500 (Internal Server Error)
            response.setStatus(500);
            out.close();
        }
    }
}
