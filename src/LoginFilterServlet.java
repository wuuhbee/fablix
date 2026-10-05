import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

//@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilterServlet implements Filter{

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        System.out.println("LoginFilter is intercepting: " + ((HttpServletRequest)request).getRequestURI());

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // prevents infinite redirect loop!
        if (isUrlAllowedWithoutLogin(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        // Prevents Caching
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setDateHeader("Expires", 0);
        if (requestURI.contains("/_dashboard/")) {
            if (EmployeeIsNotLoggedIn(session)) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard");
            } else {
                chain.doFilter(request, response);
            }
            return;
        }

        if (UserIsNotLoggedIn(session)) {
            System.out.println("Redirecting to Login");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean UserIsNotLoggedIn(HttpSession session) {
        return session == null ||
                session.getAttribute("user") == null;
    }

    private boolean EmployeeIsNotLoggedIn(HttpSession session) {
        return session == null ||
                session.getAttribute("employee") == null;
    }

    private boolean isUrlAllowedWithoutLogin(String loginUrl) {
        return loginUrl.toLowerCase().endsWith("login.html") ||
                loginUrl.toLowerCase().endsWith("/_dashboard") ||
                loginUrl.toLowerCase().endsWith("/_dashboard/") ||
                loginUrl.toLowerCase().endsWith("/_dashboard/index.html") ||
                loginUrl.toLowerCase().endsWith("/_dashboard/index.js") ||
                loginUrl.toLowerCase().endsWith("index.html") ||
                loginUrl.toLowerCase().endsWith("login.js") ||
                loginUrl.toLowerCase().endsWith("index.js") ||
                loginUrl.toLowerCase().endsWith("api/login") ||
                loginUrl.toLowerCase().contains("api/_dashboard") ||
                loginUrl.toLowerCase().endsWith("styles/styles.css");
    }
}
