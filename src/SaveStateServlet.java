import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "SaveStateServlet", urlPatterns = "/api/save-state")
public class SaveStateServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        System.out.println("SAVE STATE");
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {

            HttpSession session = request.getSession(true);

            String mode = request.getParameter("mode");
            String value = request.getParameter("value");
            String sort = request.getParameter("sort");
            String order = request.getParameter("order");
            String secSort = request.getParameter("secSort");
            String secOrder = request.getParameter("secOrder");
            String page = request.getParameter("page");
            String limit = request.getParameter("limit");

            // search params
            String searchTitle = request.getParameter("searchTitle");
            String searchYear = request.getParameter("searchYear");
            String searchDirector = request.getParameter("searchDirector");
            String searchStar = request.getParameter("searchStar");

            // store
            if (mode != null) session.setAttribute("mode", mode);
            if (value != null) session.setAttribute("value", value);
            if (sort != null) session.setAttribute("sort", sort);
            if (order != null) session.setAttribute("order", order);
            if (secSort != null) session.setAttribute("secSort", secSort);
            if (secOrder != null) session.setAttribute("secOrder", secOrder);
            if (page != null) session.setAttribute("page", page);
            if (limit != null) session.setAttribute("limit", limit);
            if (searchTitle != null) session.setAttribute("searchTitle", searchTitle);
            if (searchYear != null) session.setAttribute("searchYear", searchYear);
            if (searchDirector != null) session.setAttribute("searchDirector", searchDirector);
            if (searchStar != null) session.setAttribute("searchStar", searchStar);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status", "success");
            jsonObject.addProperty("message", "State saved");

            System.out.println("SAVE STATE SESSION ID: " + session.getId());
            System.out.println("mode: " + session.getAttribute("mode"));
            System.out.println("value: " + session.getAttribute("value"));
            System.out.println("sort: " + session.getAttribute("sort"));
            System.out.println("page: " + session.getAttribute("page"));

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