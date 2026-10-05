import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "GetStateServlet", urlPatterns = "/api/get-state")
public class GetStateServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        System.out.println("GET STATE");
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(false);

            JsonObject jsonObject = new JsonObject();

            if (session == null) {
                jsonObject.addProperty("mode", "");
                jsonObject.addProperty("value", "");
                jsonObject.addProperty("sort", "title");
                jsonObject.addProperty("order", "ASC");
                jsonObject.addProperty("secSort", "rating");
                jsonObject.addProperty("secOrder", "DESC");
                jsonObject.addProperty("page", "1");
                jsonObject.addProperty("limit", "20");
                jsonObject.addProperty("searchTitle", "");
                jsonObject.addProperty("searchYear", "");
                jsonObject.addProperty("searchDirector", "");
                jsonObject.addProperty("searchStar", "");
            } else {
                jsonObject.addProperty("mode", (String) session.getAttribute("mode"));
                jsonObject.addProperty("value", (String) session.getAttribute("value"));
                jsonObject.addProperty("sort", (String) session.getAttribute("sort"));
                jsonObject.addProperty("order", (String) session.getAttribute("order"));
                jsonObject.addProperty("secSort", (String) session.getAttribute("secSort"));
                jsonObject.addProperty("secOrder", (String) session.getAttribute("secOrder"));
                jsonObject.addProperty("page", (String) session.getAttribute("page"));
                jsonObject.addProperty("limit", (String) session.getAttribute("limit"));
                jsonObject.addProperty("searchTitle", (String) session.getAttribute("searchTitle"));
                jsonObject.addProperty("searchYear", (String) session.getAttribute("searchYear"));
                jsonObject.addProperty("searchDirector", (String) session.getAttribute("searchDirector"));
                jsonObject.addProperty("searchStar", (String) session.getAttribute("searchStar"));

                System.out.println("GET STATE SESSION ID: " + session.getId());
                System.out.println("mode: " + session.getAttribute("mode"));
                System.out.println("value: " + session.getAttribute("value"));
                System.out.println("sort: " + session.getAttribute("sort"));
                System.out.println("page: " + session.getAttribute("page"));
            }

            out.write(jsonObject.toString());

        } catch (Exception e) {
            response.setStatus(500);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());

            try (PrintWriter out = response.getWriter()) {
                out.write(jsonObject.toString());
            }
        }
    }
}