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
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
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

    private boolean credentialsVerified(String password, String encryptedPassword) {
        return new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");

        String recaptchaToken = request.getParameter("g-recaptcha-response");

        if (!RecaptchaVerifyUtils.verify(recaptchaToken)) {
            PrintWriter out = response.getWriter();
            JsonObject error = new JsonObject();
            error.addProperty("status", "fail");
            error.addProperty("message", "reCAPTCHA failed");
            out.write(error.toString());
            return;
        }

        String emailEntered = request.getParameter("email");
        String passwordEntered = request.getParameter("password");

        HttpSession session = request.getSession();

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> customerCollection = database.getCollection("customers");

            Bson filter = Filters.eq("email", emailEntered);
            Document customer = customerCollection.find(filter).first();
            if (customer == null) {
                System.out.println("Customer does not exist");
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("message", "Incorrect email or password");
                out.write(errorMsg.toString());
            } else {
                String encryptedPass = customer.getString("password");

                JsonObject confirmation = new JsonObject();
                if (credentialsVerified(passwordEntered, encryptedPass)) {
                    confirmation.addProperty("status", "success");
                    confirmation.addProperty("message", "success");

                    session.setAttribute("user",
                            new Customer(emailEntered, customer.getInteger("_id")));
                } else {
                    confirmation.addProperty("status", "fail");
                    confirmation.addProperty("message", "Incorrect username or password.");
                }
                out.write(confirmation.toString());
            }

        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error", e);

            response.setStatus(500);
            out.close();
        }
    }
}
