import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CastDOMParser {
    private Connection conn;
    private List<String> inconsistencies;
    private List<String> missingActors;
    private PrintWriter fileWriter;
    private HashMap<String, HashSet<String>> castMap;
    private int actorsAdded;
    private HashMap<String, String> actorIds;

    public CastDOMParser() {
        try {
            String jdbcUrl = System.getenv("url");
            String username = System.getenv("user");
            String password = System.getenv("pass");

            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(jdbcUrl, username, password);
            this.fileWriter = new PrintWriter(new FileWriter("cast_inconsistency_report.txt"), false);
            this.inconsistencies = new ArrayList<>();
            this.castMap = new HashMap<>(); // key = movie, value = list of cast
            this.actorIds = new HashMap<>();
            this.missingActors = new ArrayList<>();
            hashActorIds();
            this.actorsAdded = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addInconsistency(String message) {
        System.out.println(message);
        inconsistencies.add(message + "\n");
    }

    private void logInconsistency() {
        fileWriter.println(inconsistencies);
        fileWriter.flush();
    }

    void run() throws IOException, ParserConfigurationException, SAXException {
        long startTime = System.currentTimeMillis();
        Document document = parseXMLFile();
        convertDocumentIntoCast(document);
        insertMissingActorIntoStarsTable();
        insertCastInSIMTable();
        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime-startTime) + "ms");
        System.out.println("--- Cast Inconsistency Report ---");
        System.out.println(inconsistencies.size() + " actor name inconsistencies");
        System.out.println("Actors Added: " + actorsAdded);
        logInconsistency();
    }

    private Document parseXMLFile()
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        FileInputStream fis = new FileInputStream("stanford-movies/casts124.xml");
        InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.ISO_8859_1);
        InputSource is = new InputSource(reader);
        return documentBuilder.parse(is);
    }

    private void convertDocumentIntoCast(Document document) {
        Element rootDocumentElement = document.getDocumentElement();
        NodeList directorFilmsList = rootDocumentElement.getElementsByTagName("dirfilms");
        for (int i = 0; i < directorFilmsList.getLength(); i++) {
            Element directorFilms = (Element) directorFilmsList.item(i);
            NodeList filmList = directorFilms.getElementsByTagName("filmc");
            for (int j = 0; j < filmList.getLength(); j++) {
                Element singleFilm = (Element) filmList.item(j);
                processFilmToActorList(singleFilm);
            }
        }
    }

    private void processFilmToActorList(Element film) {
        NodeList castList = film.getElementsByTagName("m");
        for (int i = 0; i < castList.getLength(); i++) {
            Element singleActor = (Element) castList.item(i);
            String filmId = getTextValue(singleActor, "f");
            String actorName = getTextValue(singleActor, "a");
            if (filmId == null){
                addInconsistency("Inconsistency: element=filmId, value=null");
                return;
            }
            if (actorName == null) {
                addInconsistency("Inconsistency: element=actorName, value=null");
                return;
            }
            if (actorName.equals("s a")) {
                addInconsistency("Inconsistency: element=actorName, value=s a (some actor)");
                return;
            }
            actorName = cleanName(actorName);
            castMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(actorName);
        }
    }

    private String cleanName(String name) {
        if (name == null){
            addInconsistency("Inconsistency: element=actorName, value=null");
            return null;
        }
        return name.replace("\\'e", "é")
                .replace("\\'a", "á")
                .replace("\\'o", "ó")
                .replace("\\'i", "í")
                .replace("\\'u", "ú")
                .replace("\\`e", "è")
                .replace("\\\"e", "ë")
                .replace("~j", " J")
                .replace("~s", " S")
                .replace("~", " ")
                .replace("\\'", "'")
                .replace("`", "'")
                .replace("\\", "");
    }

    private String getTextValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent().trim();
        }
        addInconsistency("Inconsistency: element=stagename, value=null");
        return null;
    }

    private void hashActorIds(){
        String allStars = "SELECT name, id FROM stars";
        try (PreparedStatement statement = conn.prepareStatement(allStars);
             ResultSet rs = statement.executeQuery()){
            while (rs.next()) {
                String key = rs.getString("name");
                actorIds.put(key, rs.getString("id"));
            }
        } catch (SQLException e) {
            System.out.println("Error hashing all stars");
        }
    }

    private void insertCastInSIMTable() {
        String INSERT_QUERY =
                "INSERT IGNORE INTO stars_in_movies (star_id, movie_id) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(INSERT_QUERY)){
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (String movieId : castMap.keySet()) {
                HashSet<String> actors = castMap.get(movieId);
                for (String actorName : actors) {
                    String starId = actorIds.get(actorName);
                    if (starId != null) {
                        statement.setString(1, starId);
                        statement.setString(2, movieId);
                        statement.addBatch();
                        if (++batchCount % 1000 == 0) {
                            statement.executeBatch();
                        }
                        actorsAdded++;
                    } else {
                        addInconsistency("Inconsistency: Actor name not found - " + actorName);
                    }
                }
            }
            statement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
                System.out.println("SQL ERROR");
                e.printStackTrace();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            System.out.println("Error with insert cast into table.");
        }
    }

    private void insertMissingActorIntoStarsTable() {
        filterMissingActorsFromStarTable();
        insertActorIntoStarTable();
    }

    private void filterMissingActorsFromStarTable() {
        for (String movieId : castMap.keySet()) {
            HashSet<String> actors = castMap.get(movieId);
            for (String actorName : actors) {
                String starId = actorIds.get(actorName);
                if (starId == null) {
                    missingActors.add(actorName);
                }
            }
        }
    }

    private void insertActorIntoStarTable() {
        String newID = maxIdOfStarTable();
        String INSERT_QUERY = """
                INSERT IGNORE INTO stars(id, name) VALUES (?, ?);""";

        try (PreparedStatement statement = conn.prepareStatement(INSERT_QUERY)) {
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (String actorName : missingActors) {
                newID = incrementId(newID);
                statement.setString(1, newID);
                statement.setString(2, actorName);
                statement.addBatch();
                insertNewActorIntoHashIds(newID, actorName);
                if(++batchCount % 1000 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
                System.out.println("SQL ERROR");
                e.printStackTrace();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        }
    }

    private void insertNewActorIntoHashIds(String id, String actorName) {
        actorIds.put(actorName, id);
    }

    private String maxIdOfStarTable() {
        String QUERY = "SELECT MAX(id) FROM stars";
        try (PreparedStatement st = conn.prepareStatement(QUERY);
             ResultSet rs = st.executeQuery("SELECT MAX(id) FROM stars")) {
            if (rs.next() && rs.getString(1) != null) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String incrementId(String id) {
        String prefix = id.substring(0, 2);
        int number = Integer.parseInt(id.substring(2));
        int newNumber = number + 1;
        return prefix + newNumber;
    }


    public static void main(String[] args)
            throws IOException, SAXException, ParserConfigurationException {
        CastDOMParser castDOMParser = new CastDOMParser();
        castDOMParser.run();
    }
}
