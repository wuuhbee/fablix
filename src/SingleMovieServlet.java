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

import java.io.*;

// Declares a WebServlet named SingleMovieServlet, mapping to url "/api/single-movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
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

    public synchronized void logPerformance(long ts, long tj) {
        String userHome = System.getProperty("user.home");
        String filePath = userHome + File.separator + "IdeaProjects"
                + File.separator + "2026-spring-cs-122b-flicks-the-sql"
                + File.separator + "mongo-time"
                + File.separator + "single-movies.txt";

        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(ts + "," + tj);

        } catch (IOException e) {
            System.err.println("Error writing performance log metrics: "  + e.getMessage());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        long doGetStartTime = System.nanoTime();
        response.setContentType("application/json");

        // Get parameter id from url request
        String id = request.getParameter("id");

        // Debug log message in localhost log
        request.getServletContext().log("getting id: " + id);

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> movie = database.getCollection("movies");
            Bson filter = Filters.eq("_id", id);
            long queryStartTime = System.nanoTime();
            FindIterable<Document> movieFound = movie.find(filter);
            long queryEndTime = System.nanoTime();
            long tj = queryEndTime - queryStartTime;
            JsonArray movieArray = new JsonArray();
            for(Document m :movieFound) {
                String movieTitle = m.getString("title");
                int movieYear = m.getInteger("year");
                String movieDirector = m.getString("director");
                Double rating = m.getDouble("rating") != null ? m.getDouble("rating") : 0.0;


                JsonArray starArray = new JsonArray();
                for (Document star : m.getList("stars", Document.class)) {
                    JsonObject starObj = new JsonObject();
                    starObj.addProperty("star_id", star.getString("id"));
                    starObj.addProperty("star_name", star.getString("name"));
                    starArray.add(starObj);
                }

                JsonArray genreArray = new JsonArray();
                for (String genre : m.getList("genres", String.class)) {
                    JsonObject genreObj = new JsonObject();
                    genreObj.addProperty("genre_name", genre);
                    genreArray.add(genreObj);
                }

                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);
                jsonObject.add("stars", starArray);
                jsonObject.add("genres", genreArray);
                jsonObject.addProperty("rating", String.format("%.1f",rating));

                movieArray.add(jsonObject);
            }
            out.write(movieArray.toString());
            response.setStatus(200);

            long doGetEndTime = System.nanoTime();
            long ts = doGetEndTime - doGetStartTime;
            logPerformance(ts, tj);
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
