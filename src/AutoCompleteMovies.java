import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/movie-suggestion")
public class AutoCompleteMovies extends HttpServlet {

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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JsonArray jsonArray = new JsonArray();

            String query = request.getParameter("query");

            if (query == null || query.trim().isEmpty()) {
                response.getWriter().write(jsonArray.toString());
                return;
            }

            MongoCollection<Document> movieCollection = database.getCollection("movies");

            String[] tokens = query.trim().split("\\s+");
            List<Bson> conditions = new ArrayList<>();
            for (String token : tokens) {
                conditions.add(Filters.regex("title", "(?i)\\b" + Pattern.quote(token)));
            }
            Bson filter = Filters.and(conditions);

            for (Document movie : movieCollection.find(filter).limit(10)) {
                jsonArray.add(generateJsonObject(movie.getString("_id"), movie.getString("title")));
            }

            response.getWriter().write(jsonArray.toString());
        } catch (Exception e) {
            System.out.println(e);
            response.sendError(500, e.getMessage());
        }
    }

    private static JsonObject generateJsonObject(String movieID, String movieTitle) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("value", movieTitle);

        JsonObject additionalDataJsonObject = new JsonObject();
        additionalDataJsonObject.addProperty("movieID", movieID);

        jsonObject.add("data", additionalDataJsonObject);
        return jsonObject;
    }
}