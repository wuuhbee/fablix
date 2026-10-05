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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActorDOMParser {
    private Connection conn;
    private List<String> inconsistencies;
    private int stagenameInconsistency;
    private int dobInconsistency;
    private Set<String> allActors;
    private PrintWriter fileWriter;

    public ActorDOMParser() {
        try {
            String jdbcUrl = System.getenv("url");
            String username = System.getenv("user");
            String password = System.getenv("pass");

            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(jdbcUrl, username, password);
            this.fileWriter = new PrintWriter(new FileWriter("actor_inconsistency_report.txt"), false);
            this.inconsistencies = new ArrayList<>();
            this.allActors = new HashSet<>();
            this.stagenameInconsistency = 0;
            this.dobInconsistency = 0;
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
        List<Actors> actorsList = convertDocumentIntoActors(document);
        hashAllActors();
        insertActorsIntoTable(actorsList);

        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime);
        System.out.println("Time: " + totalTime + "ms");
        logInconsistency();
    }

    public void printInconsistencyReport() {
        System.out.println("--- Actor Inconsistency Report ---");
        System.out.println(stagenameInconsistency + " stagename inconsistencies");
        System.out.println(dobInconsistency + " dob inconsistencies");
        System.out.println("Total Inconsistencies: " + (stagenameInconsistency + dobInconsistency));
    }

    private Document parseXMLFile()
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        FileInputStream fis = new FileInputStream("stanford-movies/actors63.xml");
        InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.ISO_8859_1);
        InputSource is = new InputSource(reader);
        return documentBuilder.parse(is);
    }

    private List<Actors> convertDocumentIntoActors(Document document) {
        Element rootDocumentElement = document.getDocumentElement();
        List<Actors> actorsList = new ArrayList<>();
        NodeList actorNodeList = rootDocumentElement.getElementsByTagName("actor");

        int actorNodeListLength = actorNodeList.getLength();
        for (int i = 0; i < actorNodeListLength; i++) {
            Element element = (Element) actorNodeList.item(i);
            Actors actor = convertDOMElementIntoActors(element);
            actorsList.add(actor);
        }
        return actorsList;
    }

    private Actors convertDOMElementIntoActors(Element element) {
        String name = getTextValue(element, "stagename");
        String cleanedName = cleanName(name);
        int dob = getIntValue(element, "dob"); // 0 == null
        return new Actors(cleanedName, dob);
    }

    private String getTextValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent().trim();
        }
        addInconsistency("Inconsistency: element=stagename, value=null");
        stagenameInconsistency++;
        return null;
    }

    private int getIntValue(Element element, String tagName) {
        String text = getTextValue(element, tagName);
        if (text == null || text.isEmpty()){
            addInconsistency("Inconsistency: element=dob, value=null");
            dobInconsistency++;
            return 0;
        } else {
            try {
                return Integer.parseInt(getTextValue(element, tagName));
            } catch (NumberFormatException e) {
                String dob = getTextValue(element, tagName);
                addInconsistency("Inconsistency: element=dob, value=" + dob);
                dobInconsistency++;
                return 0;
            }
        }
    }

    private String cleanName(String name) {
        if (name == null){
            addInconsistency("Inconsistency: element=stagename, value=null");
            stagenameInconsistency++;
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

    private String maxIdOfStarTable(){
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

    void insertActorsIntoTable(List<Actors> actorsList) {
        String newID = maxIdOfStarTable();
        String INSERT_QUERY = """
                INSERT IGNORE INTO stars(id, name, birth_year) VALUES (?, ?, ?);""";

        try(PreparedStatement statement = conn.prepareStatement(INSERT_QUERY)) {
            conn.setAutoCommit(false);
            int batchCount = 0;

            for (Actors actor: actorsList) {
                String key = actor.getName() + "-" + actor.getBirthYear();
                if (this.allActors.contains(key)) continue;

                newID = incrementId(newID);
                statement.setString(1, newID);
                statement.setString(2, actor.getName());
                if (actor.getBirthYear() == 0){
                    statement.setNull(3, Types.INTEGER);
                }
                else {
                    statement.setInt(3, actor.getBirthYear());
                }
                statement.addBatch();
                if (++batchCount % 1000 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            conn.commit();
            System.out.println("Finished Inserting Stars.");

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
                System.out.println("SQL ERROR");
                e.printStackTrace();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
        }
    }

    private void hashAllActors() {
        String SELECT_ALL_QUERY = """
                SELECT name, birth_year FROM stars;""";

        try (PreparedStatement statement = conn.prepareStatement(SELECT_ALL_QUERY)){
            ResultSet allStars = statement.executeQuery();
            while (allStars.next()){
                this.allActors.add(
                        allStars.getString("name") + "-" + allStars.getString("birth_year"));
            }
            allStars.close();
            System.out.println("Successfully hashed all actors");
        } catch (Exception e) {
            System.out.println("Error hashing all actors");
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
            throws IOException, SAXException, ParserConfigurationException {
        ActorDOMParser actorDOMParser = new ActorDOMParser();
        actorDOMParser.run();
    }
}
