import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(name = "NewSearchServlet", urlPatterns = "/api/gen-search")
public class NewSearchServlet extends HttpServlet {
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

    // be sure to add the following in the command line:
    // db.movies.createIndex({title: "text"});
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        String queryParam = request.getParameter("q");

        int page = 1;
        int limit = 20;

        String pageUrl = request.getParameter("page");
        String limitUrl = request.getParameter("limit");

        if (pageUrl != null) { page = Integer.parseInt(pageUrl); }
        if (limitUrl != null) { limit = Integer.parseInt(limitUrl); }

        int offset = (page - 1) * limit; // skip num pages

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

        JsonArray results = new JsonArray();
        try (PrintWriter out = response.getWriter()) {
            if (queryParam == null || queryParam.trim().isEmpty()) {
                out.write(results.toString());
                return;
            }

            MongoCollection<Document> movieCollection = database.getCollection("movies");

            String[] keywords = queryParam.trim().split("\\s+");
            List<Bson> prefixFilters = new ArrayList<>();
            for (String keyword : keywords) {
                if (!keyword.isEmpty()) {
                    String regexPattern = "\\b" + java.util.regex.Pattern.quote(keyword);

                    Bson singleWordFilter = Filters.regex("title", regexPattern, "i");
                    prefixFilters.add(singleWordFilter);
                }
            }

            Bson textFilter;
            if (prefixFilters.size() == 1) {
                textFilter = prefixFilters.get(0);
            } else if (prefixFilters.size() > 1) {
                textFilter = Filters.and(prefixFilters);
            } else {
                textFilter = new Document();
            }

            FindIterable<Document> movies = movieCollection.find(textFilter)
                    .sort(sorting)
                    .skip(offset)
                    .limit(limit);
            System.out.println(movieCollection.countDocuments(textFilter));

            for (Document movie : movies) {
                System.out.println("Found Movies");
                JsonObject jsonMovie = new JsonObject();
                jsonMovie.addProperty("id", movie.getString("_id"));
                jsonMovie.addProperty("title", movie.getString("title"));
                jsonMovie.addProperty("year", movie.getInteger("year"));
                jsonMovie.addProperty("director", movie.getString("director"));
                Double rate = movie.getDouble("rating") != null ? movie.getDouble("rating") : 0.0;
                jsonMovie.addProperty("rating", String.format("%.1f", rate));                JsonArray jsonStars = new JsonArray();
                for (Document star : movie.getList("stars", Document.class)) {
                    JsonObject jsonStar = new JsonObject();
                    jsonStar.addProperty("id", star.getString("id"));
                    jsonStar.addProperty("name", star.getString("name"));
                    jsonStars.add(jsonStar);
                    if (jsonStars.size() >= 3) {
                        break;
                    }
                }
                jsonMovie.add("stars", jsonStars);
                results.add(jsonMovie);
            }
            out.write(results.toString());
            System.out.println("Wrote movies to JSON");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            System.out.println("Failed to find movies");
        }
    }
}
