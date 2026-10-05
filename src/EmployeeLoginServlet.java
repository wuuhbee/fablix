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
import org.bson.conversions.Bson;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/_dashboard-login")
public class EmployeeLoginServlet extends HttpServlet {
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

        String emailEntered = request.getParameter("email");
        String passwordEntered = request.getParameter("password");

        HttpSession session = request.getSession();

        try (PrintWriter out = response.getWriter()) {
            MongoCollection<Document> employeeCollection = database.getCollection("employees");

            Bson filter = Filters.eq("_email", emailEntered);
            Document employee = employeeCollection.find(filter).first();
            if (employee == null){
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("message", "Incorrect email or password");
                out.write(errorMsg.toString());
            }
            else {
                String encryptedPassword = employee.getString("password");
                JsonObject returnMsg = new JsonObject();
                if (credentialsVerified(passwordEntered, encryptedPassword)){
                    returnMsg.addProperty("status", "success");
                    returnMsg.addProperty("message", "success");
                    session.setAttribute("employee",
                            new Employee(emailEntered, employee.getString("full_name")));
                }
                else {
                    returnMsg.addProperty("status", "fail");
                    returnMsg.addProperty("message", "Incorrect email or password");
                }
                out.write(returnMsg.toString());
            }
        } catch (Exception e){
            PrintWriter out = response.getWriter();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error", e);

            response.setStatus(500);
            out.close();
        }

    }
}
