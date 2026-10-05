import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Types;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;

@WebServlet(name ="AddStarServlet", urlPatterns = "/_dashboard/api/add-star")
public class AddStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource =
                    (DataSource) new InitialContext()
                            .lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");

        String name = request.getParameter("fullName");
        String birthYear = request.getParameter("birthYear");

        String INSERT_STAR_QUERY = "{call add_star(?, ?, ?)}";

        try (Connection conn = dataSource.getConnection();
             PrintWriter out = response.getWriter();
             CallableStatement cs = conn.prepareCall(INSERT_STAR_QUERY)) {

            cs.setString(1, name);
            if (birthYear.isEmpty()){
                cs.setNull(2, Types.INTEGER);
            } else {
                cs.setInt(2, Integer.parseInt(birthYear));
            }

            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();

            String newId = cs.getString(3);
//            System.out.println("SUCCESS in getting id " + newId);

            JsonObject returnMsg = new JsonObject();
            returnMsg.addProperty("new_star_id", newId);
            returnMsg.addProperty("status", "success");
            returnMsg.addProperty("message", "success");
            out.write(returnMsg.toString());
        } catch (Exception e) {
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
