import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MovieDOMParser {

    private MongoDatabase database;
    private int moviesParsed = 0;
    private int genresParsed = 0;
    private int genresInMoviesParsed = 0;

    private int dirInconsistency = 0;
    private int catInconsistency = 0;
    private int titleInconsistency = 0;
    private int yearInconsistency = 0;
    private int fidInconsistency = 0;

    private Set<String> allGenres = new HashSet<>();
    private List<Object[]> movieBatch = new ArrayList<>();
    private List<String> genreBatch = new ArrayList<>();
    private List<Object[]> genreInMovieBatch = new ArrayList<>();

    private PrintWriter reportWriter;

    MovieDOMParser(MongoDatabase database) throws IOException {
        this.database = database;
        this.reportWriter = new PrintWriter(new FileWriter("movie_inconsistency_report.txt", false));
        hashGenresAndIds();
    }

    private void logInconsistency(String message) {
        System.out.println(message);
        reportWriter.println(message);
        reportWriter.flush();
    }

    private String normalizeGenre(String genre) {
        if (genre == null) return null;
        switch (genre.trim().toLowerCase()) {
            case "actn": case "act": case "axtn": return "Action";
            case "advt": return "Adventure";
            case "cart": return "Animation";
            case "bio": case "biob": case "biog": case "biop": case "biopp": case "biopx": return "Biography";
            case "comd": case "comdx": return "Comedy";
            case "crim": return "Crime";
            case "docu": case "dicu": case "duco": case "ducu": case "docu dram": case "dram docu": return "Documentary";
            case "dram": case "draam": case "dramd": case "dramn": case "dram>": case "anti-dram": case "dram.actn": return "Drama";
            case "epic": return "Epic";
            case "fant": return "Fantasy";
            case "faml": case "family": return "Family";
            case "hist": return "History";
            case "horr": case "hor": return "Horror";
            case "musc": case "muscl": case "muusc": case "music": case "stage musical": return "Musical";
            case "myst": case "mystp": return "Mystery";
            case "noir": case "comd noir": case "noir comd": case "noir comd romt": return "Noir";
            case "camp": return "Camp";
            case "cult": return "Cult";
            case "sati": return "Satire";
            case "romt": case "romtx": case "ront": case "romt actn": case "romt comd": case "romt dram": case "romt fant": case "romt. comd": case "romtadvt": return "Romance";
            case "scfi": case "scif": case "s.f.": case "sxfi": return "Sci-Fi";
            case "surl": case "surr": case "surreal": return "Surreal";
            case "susp": return "Thriller";
            case "psyc": case "psych dram": return "Psychology";
            case "west": case "west1": case "comd west": return "Western";
            case "viol": return "Violence";
            case "war": return "War";
            case "tv": case "tvmini": return "TV";
            case "natu": return "Nature";
            case "disat": case "disatxx": case "dist": case "disa": return "Disaster";
            case "avga": return "Avant Garde";
            case "porn": return "Porn";
            case "kinky": return "Kinky";

            // all other:
            case "allegory": case "weird": case "road": case "expm": case "undr":
            case "txx": case "art video": case "rfp; h*": case "ctxxx": case "ctcxx":
            case "ctxx": case "cond": case "rfp": case "scat": case "homo": case "porb":
            case "verite": case "ca": case "adct": case "adctx": case "sctn": case "cmr":
            case "cnr": case "cnrb": case "cnrbb": case "ram": case "h**": case "h0":
            case "fanth*": case "h": return "Other";
            default: return genre.trim();
        }
    }

    void runParser() throws IOException, ParserConfigurationException, SAXException {
        long startTime = System.currentTimeMillis();

        org.w3c.dom.Document document = parseXmlFile();
        processDocument(document);

        batchInsertMovies();
        System.out.println("Movies batch done: " + (System.currentTimeMillis() - startTime) + "ms");
        batchInsertGenres();
        System.out.println("Genres batch done: " + (System.currentTimeMillis() - startTime) + "ms");
        batchInsertGenreInMovies();
        System.out.println("Genre-movies batch done: " + (System.currentTimeMillis() - startTime) + "ms");

        long endTime = System.currentTimeMillis();

        System.out.println("Movies parsed: " + moviesParsed);
        System.out.println("Genres parsed: " + genresParsed);
        System.out.println("Genres in movies parsed: " + genresInMoviesParsed);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");

        reportWriter.close();
    }

    public void printInconsistencyReport() {
        System.out.println("--- Movies Inconsistency Report ---");
        System.out.println(catInconsistency + " category inconsistencies");
        System.out.println(dirInconsistency + " director name inconsistencies");
        System.out.println(yearInconsistency + " year inconsistencies");
        System.out.println(titleInconsistency + " title inconsistencies");
        System.out.println(fidInconsistency + " film id inconsistencies");
        System.out.println("Total Inconsistencies: " +
                (catInconsistency + dirInconsistency + yearInconsistency + titleInconsistency + fidInconsistency));
    }

    private org.w3c.dom.Document parseXmlFile() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new org.xml.sax.InputSource(
                new java.io.InputStreamReader(
                        new java.io.FileInputStream("stanford-movies/mains243.xml"), "ISO-8859-1")));
    }

    private void processDocument(org.w3c.dom.Document document) {
        Element root = document.getDocumentElement(); // <movies>

        NodeList directorFilmsList = root.getElementsByTagName("directorfilms");
        for (int i = 0; i < directorFilmsList.getLength(); i++) {
            Element directorFilms = (Element) directorFilmsList.item(i);
            String directorName = getTextValue(directorFilms, "dirname");

            NodeList filmList = directorFilms.getElementsByTagName("film");
            for (int j = 0; j < filmList.getLength(); j++) {
                Element film = (Element) filmList.item(j);
                processFilm(film, directorName);
            }
        }
    }

    private void processFilm(Element film, String directorName) {
        String fid = getTextValue(film, "fid");
        String title = getTextValue(film, "t");
        String yearStr = getTextValue(film, "year");

        if (fid == null) {
            logInconsistency("Inconsistency: element=fid, value=null");
            fidInconsistency++;
            return;
        }
        if (title == null) {
            logInconsistency("Inconsistency: element=t, value=null");
            titleInconsistency++;
            return;
        }

        Integer year = null;
        if (yearStr == null) {
            logInconsistency("Inconsistency: element=year, value=null, skipping fid=" + fid);
            yearInconsistency++;
            return;
        }

        try {
            year = Integer.parseInt(yearStr.trim());
        } catch (NumberFormatException e) {
            logInconsistency("Inconsistency (not a valid integer): element=year, value=" + yearStr + ", skipping fid=" + fid);
            yearInconsistency++;
            return;
        }

        if (directorName == null) {
            logInconsistency("Inconsistency: element=dirname, value=null");
            dirInconsistency++;
            return;
        }

        if (fid.trim().isEmpty()) {
            logInconsistency("Inconsistency: element=fid, value=empty");
            fidInconsistency++;
            return;
        }

        if (title.trim().isEmpty()) {
            logInconsistency("Inconsistency: element=t, value=empty");
            titleInconsistency++;
            return;
        }

        if (directorName.trim().isEmpty()) {
            logInconsistency("Inconsistency: element=dirname, value=empty, skipping fid=" + fid);
            dirInconsistency++;
            return;
        }

        movieBatch.add(new Object[]{fid, title, year, directorName});

        NodeList catList = film.getElementsByTagName("cat");
        for (int i = 0; i < catList.getLength(); i++) {
            if (catList.item(i).getFirstChild() != null) {
                String genreName = catList.item(i).getFirstChild().getNodeValue();
                if (genreName != null && !genreName.trim().isEmpty()) {
                    genreName = normalizeGenre(genreName.trim());
                    if (!genreBatch.contains(genreName) && !allGenres.contains((genreName))) {
                        genreBatch.add(genreName);
                    }
                    genreInMovieBatch.add(new Object[]{genreName, fid});
                } else {
                    logInconsistency("Inconsistency: element=cat, value=null, fid=" + fid);
                    catInconsistency++;
                }
            }
        }
    }

    private void hashGenresAndIds() {
        MongoCollection<Document> genreCollection = database.getCollection("genres");

        try {
            for (Document genre : genreCollection.find()) {
                String name = genre.getString("name");
                if (name != null) {
                    this.allGenres.add(name);
                }
            }
            System.out.println("Successfully hashed all genres");
        } catch (Exception e) {
            System.out.println("Error hashing all genres");
            e.printStackTrace();
        }
    }

    private void batchInsertMovies() {
        MongoCollection<Document> movieCollection = database.getCollection("movies");

        List<Document> docs = new ArrayList<>();
        for (Object[] movie : movieBatch) {
            //skip if already exists
            if (movieCollection.find(Filters.eq("_id", (String) movie[0])).first() != null) continue;

            Document doc = new Document("_id", (String) movie[0])
                    .append("title", (String) movie[1])
                    .append("year", (Integer) movie[2])
                    .append("director", (String) movie[3])
                    .append("genres", new ArrayList<String>())
                    .append("stars", new ArrayList<Document>());
            docs.add(doc);
            moviesParsed++;
        }

        if (!docs.isEmpty()) {
            movieCollection.insertMany(docs);
        }
    }

    private void batchInsertGenres() {
        MongoCollection<Document> genreCollection = database.getCollection("genres");

        List<Document> docs = new ArrayList<>();
        for (String genreName : genreBatch) {
            if (genreCollection.find(Filters.eq("name", genreName)).first() != null) continue;

            Document doc = new Document("name", genreName);
            docs.add(doc);
            genresParsed++;
        }

        if (!docs.isEmpty()) {
            genreCollection.insertMany(docs);
        }
    }

    private void batchInsertGenreInMovies() {
        MongoCollection<Document> movieCollection = database.getCollection("movies");

        for (Object[] link : genreInMovieBatch) {
            String genreName = (String) link[0];
            String movieId = (String) link[1];

            movieCollection.updateOne(
                    Filters.and(
                            Filters.eq("_id", movieId),
                            Filters.ne("genres", genreName)
                    ),
                    new Document("$push", new Document("genres", genreName))
            );
            genresInMoviesParsed++;
        }
    }

    private String getTextValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String user = System.getenv("mongouser");
        String pass = System.getenv("mongopass");
        String uri = "mongodb://" + user + ":" + pass + "@localhost:27017/moviedb";

        com.mongodb.client.MongoClient mongoClient = com.mongodb.client.MongoClients.create(uri);
        MongoDatabase database = mongoClient.getDatabase("moviedb");
        System.out.println("MongoDB Connection Pool Initialized!");

        MovieDOMParser parser = new MovieDOMParser(database);
        parser.runParser();

        mongoClient.close();
    }
}
