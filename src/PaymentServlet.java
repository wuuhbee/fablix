import com.google.gson.JsonObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@WebServlet(name ="PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet{
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditNumber = request.getParameter("creditCardNum");
        String expDate = request.getParameter("expDate");

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> customerInfo = database.getCollection("customers");

            Bson filter = Filters.and(
                    Filters.eq("first_name", firstName),
                    Filters.eq("last_name", lastName));

            FindIterable<Document> customers = customerInfo.find(filter);
            for (Document singleCustomer : customers) {
                int customer_id = singleCustomer.getInteger("_id");
                Document credit_card = singleCustomer.get("credit_cards", Document.class);
                    if (credit_card.getString("id").equals(creditNumber) &&
                            credit_card.getString("expiration").equals(expDate)) {
                        HttpSession session = request.getSession();
                        HashMap<String, Integer> cart = (HashMap<String, Integer>) session.getAttribute("cart");
                        if (cart != null) {
                            System.out.println("Adding to Sales Table...");
                            List<Document> allSales = new ArrayList<>();
                            for (String movieId : cart.keySet()) {
                                Document sale = new Document("customer_id", customer_id)
                                        .append("movie_id", movieId)
                                        .append("sale_date", Date.valueOf(LocalDate.now()))
                                        .append("quantity", cart.get(movieId));
                                allSales.add(sale);
                            }
                            MongoCollection<Document> sales = database.getCollection("sales");
                            sales.insertMany(allSales);
                            session.setAttribute("cart", new HashMap<String, Integer>());
                        }
                        JsonObject confirmation = new JsonObject();
                        confirmation.addProperty("status", "success");
                        confirmation.addProperty("message", "Payment Authorized.");
                        out.write(confirmation.toString());
                    } else {
                        System.out.println("Payment Denied");
                        JsonObject confirmation = new JsonObject();
                        confirmation.addProperty("status", "fail");
                        confirmation.addProperty("message", "Payment Failed.");
                        out.write(confirmation.toString());
                    }
            }

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
