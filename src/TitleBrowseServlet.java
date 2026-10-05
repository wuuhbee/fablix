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
import java.util.Arrays;
import java.util.List;

// WebServlet called GenreBrowseServlet, which maps to /api/genre-browse
@WebServlet(name = "TitleBrowseServlet", urlPatterns = "/api/title-browse")
public class TitleBrowseServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

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

        // get parameters from url
        String firstChar = request.getParameter("char");
        String sort = request.getParameter("sort"); // will be the title or rating
        String secSort = request.getParameter("secSort"); // will be the opposite of sort
        String order = request.getParameter("order"); // ASC or DESC
        String secOrder = request.getParameter("secOrder"); // ASC or DESC

        // Simple error checking to make sure sort and order are set and valid
        List<String> validColumns = Arrays.asList("title", "rating");
        List<String> validOrders = Arrays.asList("ASC", "DESC");

        if (!validColumns.contains(sort)) sort = "title";
        if (!validColumns.contains(secSort)) secSort = sort.equals("title") ? "rating" : "title";
        if (!validOrders.contains(order)) order = "ASC";
        if (!validOrders.contains(secOrder)) secOrder = "ASC";

        request.getServletContext().log("getting title: " + firstChar);

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> movieCollection = database.getCollection("movies");

            Bson filter = Filters.regex("title", "^" + firstChar);
            Bson sorting = Sorts.orderBy(Sorts.descending("rating"), Sorts.ascending("title"));
            if ("title".equals(sort)) {
                Bson primary = "DESC".equals(order) ? Sorts.descending("title") : Sorts.ascending("title");
                Bson secondary = "DESC".equals(secOrder) ? Sorts.descending("rating") : Sorts.ascending("rating");
                sorting = Sorts.orderBy(primary, secondary);
            } else if ("rating".equals(sort)) {
                Bson primary = "ASC".equals(order) ? Sorts.ascending("rating") : Sorts.descending("rating");
                Bson secondary = "DESC".equals(secOrder) ? Sorts.descending("title") : Sorts.ascending("title");
                sorting = Sorts.orderBy(primary, secondary);
            }

            FindIterable<Document> movies = movieCollection.find(filter)
                    .sort(sorting)
                    .skip(offset)
                    .limit(limit);

            JsonArray movieResults = new JsonArray();
            for (Document movie: movies) {
                JsonObject jsonMovie = new JsonObject();
                jsonMovie.addProperty("id", movie.getString("_id"));
                jsonMovie.addProperty("title", movie.getString("title"));
                jsonMovie.addProperty("year", movie.getInteger("year"));
                jsonMovie.addProperty("director", movie.getString("director"));
                Double rate = movie.getDouble("rating") != null ? movie.getDouble("rating") : 0.0;
                jsonMovie.addProperty("rating", String.format("%.1f",rate));

                JsonArray jsonGenres = new JsonArray();
                List<String> mongoGenres = movie.getList("genres", String.class);
                if (mongoGenres != null) {
                    for (String genres : mongoGenres) {
                        jsonGenres.add(genres);
                    }
                }
                jsonMovie.add("genres", jsonGenres);

                JsonArray jsonStars = new JsonArray();
                List<Document> mongoStars = movie.getList("stars", Document.class);
                if (mongoStars != null) {
                    int starCount = 0;
                    for (Document stars : mongoStars) {
                        if (starCount >= 3) break;
                        JsonObject star = new JsonObject();
                        star.addProperty("id", stars.getString("id"));
                        star.addProperty("name", stars.getString("name"));
                        star.addProperty("birth_year", stars.getInteger("birth_year"));
                        jsonStars.add(star);
                        starCount++;
                    }
                }
                jsonMovie.add("stars", jsonStars);
                movieResults.add(jsonMovie);
            }


//            movieStatement.setString(1, firstChar + "%");
//            ResultSet movieSet = movieStatement.executeQuery();
//
//            JsonArray jsonArray = new JsonArray();
//
//            while (movieSet.next()) {
//                String movieID = movieSet.getString("id");
//                String movieTitle = movieSet.getString("title");
//                int movieYear = movieSet.getInt("year");
//                String movieDirector = movieSet.getString("director");
//                Float movieRating;
//                if (movieSet.getObject("rating") != null) {
//                    movieRating = movieSet.getFloat("rating");
//                } else {
//                    movieRating = 0.0f;
//                }
//
//                genreStatement.setString(1, movieID);
//                ResultSet genreSet = genreStatement.executeQuery();
//
//                JsonArray genreArray = new JsonArray();
//                while (genreSet.next()) {
//                    genreArray.add(genreSet.getString("name"));
//                }
//                genreSet.close();
//
//                starsStatement.setString(1, movieID);
//                ResultSet starsSet = starsStatement.executeQuery();
//
//                JsonArray starArray = new JsonArray();
//                while (starsSet.next()){
//                    JsonObject starObj = new JsonObject();
//                    starObj.addProperty("id", starsSet.getString("id"));
//                    starObj.addProperty("name", starsSet.getString("name"));
//                    starArray.add(starObj);
//                }
//                starsSet.close();
//
//                JsonObject jsonObject = new JsonObject();
//                jsonObject.addProperty("id", movieID);
//                jsonObject.addProperty("title", movieTitle);
//                jsonObject.addProperty("year", movieYear);
//                jsonObject.addProperty("director", movieDirector);
//                jsonObject.addProperty("rating", movieRating);
//                jsonObject.add("genres", genreArray);
//                jsonObject.add("stars", starArray);
//
//                jsonArray.add(jsonObject);
//            }
            out.write(movieResults.toString());
            response.setStatus(200);

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
