import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

@WebServlet(name ="GetCartServlet", urlPatterns = "/api/get-cart")
public class GetCartServlet extends HttpServlet {

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

        HttpSession session = request.getSession();
        HashMap<String, Integer> cart = (HashMap<String, Integer>) session.getAttribute("cart");

        try (PrintWriter out = response.getWriter()) {

            MongoCollection<Document> movieCollection = database.getCollection("movies");

            double total = 0.0;
            if (cart != null) {
                for (String movieId : cart.keySet()) {
                    Document movie = movieCollection.find(Filters.eq("_id", movieId)).first();
                    if (movie != null && movie.getDouble("price") != null) {
                        total += movie.getDouble("price") * cart.get(movieId);
                    }
                }
            }

            JsonObject cartTotal = new JsonObject();
            cartTotal.addProperty("price", total);
            out.write(cartTotal.toString());

        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status", "fail");
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error", e);
            response.setStatus(500);
            out.close();

        }

    }
}
