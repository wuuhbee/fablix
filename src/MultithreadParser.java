import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultithreadParser {

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    static {
        String user = System.getenv("mongouser");
        String pass = System.getenv("mongopass");
        String uri = "mongodb://" + user + ":" + pass + "@localhost:27017/moviedb";
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase("moviedb");
        System.out.println("MongoDB Connection Pool Initialized!");
    }

    static class MovieParserWorker implements Runnable {
        @Override
        public void run() {
            try {
                MovieDOMParser movieParser = new MovieDOMParser(database);
                movieParser.runParser();
                movieParser.printInconsistencyReport();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ActorParserWorker implements Runnable {
        @Override
        public void run() {
            try {
                ActorDOMParser actorParser = new ActorDOMParser();
                actorParser.run();
                actorParser.printInconsistencyReport();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class CastParserWorker implements Runnable {
        @Override
        public void run() {
            try {
                CastDOMParser castParser = new CastDOMParser();
                castParser.run();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new MovieParserWorker());
        executor.execute(new ActorParserWorker());
        executor.shutdown();

        while (!executor.isTerminated()) {}
        System.out.println("Movies and Actors finished\n");


        ExecutorService castExecutor = Executors.newFixedThreadPool(1);
        castExecutor.execute(new CastParserWorker());
        castExecutor.shutdown();

        while (!castExecutor.isTerminated()) {}
        System.out.println("Cast finished");
        System.out.println("All parsers finished");
        System.out.println("TOTAL TIME FOR ALL PARSERS: " + (System.currentTimeMillis() - startTime) + "ms");

        mongoClient.close();
    }
}