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

import java.io.*;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

// Declaring a WebServlet called SearchServlet, which maps to url "/api/search"
@WebServlet(name = "SearchServlet", urlPatterns = "/api/search")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

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
                + File.separator + "search-movies.txt";

        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(ts + "," + tj);

        } catch (IOException e) {
            System.err.println("Error writing performance log metrics: "  + e.getMessage());
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long doGetStartTime = System.nanoTime();
        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();


        // Pagination updates, get page and limit from URL in HTTP request
        int page = 1;
        int limit = 20;

        String pageUrl = request.getParameter("page");
        String limitUrl = request.getParameter("limit");

        if (pageUrl != null && !pageUrl.isEmpty()) {
            page = Integer.parseInt(pageUrl);
        }
        if (limitUrl != null && !limitUrl.isEmpty()) {
            limit = Integer.parseInt(limitUrl);
        }

        if (limit > 100) limit = 100;

        int offset = (page - 1) * limit; // skip num pages
        // page 2: skip (2-1) * N results per page


        // Search
        String title = request.getParameter("title");
        String yearUrl = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");

        String sort = request.getParameter("sort");
        String secSort = request.getParameter("secSort");
        String order = request.getParameter("order");
        String secOrder = request.getParameter("secOrder");

        List<String> validColumns = Arrays.asList("title", "rating");
        List<String> validOrders = Arrays.asList("ASC", "DESC");

        if (!validColumns.contains(sort)) sort = "title";
        if (!validColumns.contains(secSort)) secSort = "rating";
        if (!validOrders.contains(order)) order = "ASC";
        if (!validOrders.contains(secOrder)) secOrder = "ASC";

        Integer year = null;

        if (yearUrl != null && !yearUrl.trim().isEmpty()) {
            year = Integer.parseInt(yearUrl.trim());
        }

        if (title == null) title = ""; else title = title.trim();
        if (director == null) director = ""; else director = director.trim();
        if (star == null) star = ""; else star = star.trim();

        try {
            MongoCollection<Document> movieCollection = database.getCollection("movies");

            List<Bson> filters = new java.util.ArrayList<>();

            // substring match
            if (!title.isBlank()) {
                filters.add(Filters.regex("title", Pattern.compile(title, Pattern.CASE_INSENSITIVE)));
            }

            // exact match only
            if (year != null) {
                filters.add(Filters.eq("year", year));
            }


            if (!director.isBlank()) {
                filters.add(Filters.regex("director", Pattern.compile(director, Pattern.CASE_INSENSITIVE)));
            }

            // search stars
            if (!star.isBlank()) {
                filters.add(Filters.regex("stars.name", Pattern.compile(star, Pattern.CASE_INSENSITIVE)));
            }

            Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);

            //sorting
            Bson sorting;
            if ("title".equals(sort)) {
                Bson primary = "DESC".equals(order) ? Sorts.descending("title") : Sorts.ascending("title");
                Bson secondary = "DESC".equals(secOrder) ? Sorts.descending("rating") : Sorts.ascending("rating");
                sorting = Sorts.orderBy(primary, secondary);
            } else {
                Bson primary = "ASC".equals(order) ? Sorts.ascending("rating") : Sorts.descending("rating");
                Bson secondary = "DESC".equals(secOrder) ? Sorts.descending("title") : Sorts.ascending("title");
                sorting = Sorts.orderBy(primary, secondary);
            }
            long queryStartTime = System.nanoTime();
            FindIterable<Document> movies = movieCollection.find(filter)
                    .sort(sorting)
                    .skip(offset)
                    .limit(limit);
            long queryEndTime = System.nanoTime();
            long tj = queryEndTime - queryStartTime;
            JsonArray movieJsonArray = new JsonArray();

            for (Document movie : movies) {
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("id", movie.getString("_id"));
                movieJson.addProperty("title", movie.getString("title"));
                movieJson.addProperty("year", movie.getInteger("year"));
                movieJson.addProperty("director", movie.getString("director"));
                Double rate = movie.getDouble("rating") != null ? movie.getDouble("rating") : 0.0;
                movieJson.addProperty("rating", String.format("%.1f", rate));

                JsonArray jsonGenres = new JsonArray();
                List<String> mongoGenres = movie.getList("genres", String.class);
                if (mongoGenres != null) {
                    for (String genre : mongoGenres) {
                        jsonGenres.add(genre);
                    }
                }
                movieJson.add("genres", jsonGenres);

                //first 3 stars
                JsonArray jsonStars = new JsonArray();
                List<Document> mongoStars = movie.getList("stars", Document.class);
                if (mongoStars != null) {
                    int starCount = 0;
                    for (Document starDoc : mongoStars) {
                        if (starCount >= 3) break;
                        JsonObject starObj = new JsonObject();
                        starObj.addProperty("id", starDoc.getString("id"));
                        starObj.addProperty("name", starDoc.getString("name"));
                        jsonStars.add(starObj);
                        starCount++;
                    }
                }
                movieJson.add("stars", jsonStars);

                movieJsonArray.add(movieJson);
            }

            // Write JSON string to output
            out.write(movieJsonArray.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);
            long doGetEndTime = System.nanoTime();
            long ts = doGetEndTime - doGetStartTime;
            logPerformance(ts, tj);
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