import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name= "TableMetaDataServlet", urlPatterns = "/_dashboard/api/get-metadata")
public class TableMetaDataServlet extends HttpServlet {
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

    private JsonArray getTableMetaData(Connection conn)
            throws SQLException {
        JsonArray tableMetaData = new JsonArray();
        DatabaseMetaData metaData = conn.getMetaData();

        try(ResultSet allTableData =
                    metaData.getTables("moviedb", null, "%", new String[]{"TABLE"})) {
            while (allTableData.next()){
                JsonObject tableInfo = new JsonObject();
                String tableName = allTableData.getString("TABLE_NAME");
                tableInfo.addProperty("tableName", tableName);

                JsonArray tableColumns = new JsonArray();
                try (ResultSet columns = metaData.getColumns("moviedb", null, tableName, "%")) {
                    while (columns.next()){
                        JsonObject columnInfo = new JsonObject();
                        columnInfo.addProperty("columnName", columns.getString("COLUMN_NAME"));
                        columnInfo.addProperty("columnType", columns.getString("TYPE_NAME"));
                        tableColumns.add(columnInfo);
                    }
                }
                tableInfo.add("columns", tableColumns);
                tableMetaData.add(tableInfo);
            }
        }

        return tableMetaData;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        try (Connection conn = dataSource.getConnection();
             PrintWriter out = response.getWriter()) {
            JsonArray tableMetaData = getTableMetaData(conn);
            out.write(tableMetaData.toString());
        } catch (Exception e) {
            response.setStatus(500);
        }
    }
}
