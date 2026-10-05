import com.google.gson.JsonArray;
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

@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {

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

        System.out.println("CART GET!!!");
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("cart") == null) {
                out.write(new JsonArray().toString());
                return;
            }

            HashMap<String, Integer> cart = (HashMap<String, Integer>) session.getAttribute("cart");

            JsonArray jsonArray = new JsonArray();

            MongoCollection<Document> movieCollection = database.getCollection("movies");

            for (String movieId : cart.keySet()) {
                Document movie = movieCollection.find(Filters.eq("_id", movieId)).first();

                if (movie != null) {
                    JsonObject item = new JsonObject();
                    item.addProperty("id", movie.getString("_id"));
                    item.addProperty("title", movie.getString("title"));
                    item.addProperty("price", movie.getDouble("price") != null ? movie.getDouble("price") : 0.0);
                    item.addProperty("quantity", cart.get(movieId));
                    jsonArray.add(item);
                }
            }

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            response.setStatus(500);
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error", e);

            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        System.out.println("CART POST!!!");
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(true);

            HashMap<String, Integer> cart = (HashMap<String, Integer>) session.getAttribute("cart");

            if (cart == null) {
                cart = new HashMap<>();
                session.setAttribute("cart", cart);
            }

            String action = request.getParameter("action");
            String movieId = request.getParameter("movieId");

            System.out.println("action: " + action + " movieId: " + movieId);

            JsonObject jsonObject = new JsonObject();

            if (action == null || movieId == null) {
                response.setStatus(400);
                jsonObject.addProperty("errorMessage", "Missing action or movieId");
                out.write(jsonObject.toString());
                return;
            }

            switch (action) {
                case "add":
                    // +1 or =1 if not in cart
                    cart.put(movieId, cart.getOrDefault(movieId, 0) + 1);

                    jsonObject.addProperty("status", "success");
                    jsonObject.addProperty("message", "Added to cart");
                    break;

                case "update":
                    String quantityParam = request.getParameter("quantity");

                    if (quantityParam != null) {
                        int quantity = Integer.parseInt(quantityParam);

                        if (quantity <= 0) {
                            cart.remove(movieId);
                        }
                        else {
                            cart.put(movieId, quantity);
                        }

                    }
                    jsonObject.addProperty("status", "success");
                    jsonObject.addProperty("message", "Cart updated");
                    break;

                case "delete":
                    cart.remove(movieId);
                    jsonObject.addProperty("status", "success");
                    jsonObject.addProperty("message", "Removed from cart");
                    break;

                default:
                    response.setStatus(400);
                    jsonObject.addProperty("errorMessage", "Invalid action");
                    break;
            }

            session.setAttribute("cart", cart);
            System.out.println("cart size: " + cart.size());

            out.write(jsonObject.toString());
            response.setStatus(200);

        } catch (Exception e) {
            response.setStatus(500);
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error", e);

            out.close();
        }
    }
}