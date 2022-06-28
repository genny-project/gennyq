package life.genny.utils;

import life.genny.qwandautils.GennySettings;
import org.jboss.logging.Logger;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class SyncEntityThread extends Thread {
    private static final Logger log = Logger.getLogger(SyncEntityThread.class);

    String authToken;
    String realm;

    public SyncEntityThread(String authToken, String realm) {
        this.authToken = authToken;
        this.realm = realm;
    }

    private void logResponse(String url, String response) {
        log.info("Response:" + response + " from url:" + url);
    }

    private String sendGet(String url, String token) throws ExecutionException, InterruptedException {
        // create a client
        HttpClient client = HttpClient.newHttpClient();

        // create a request
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(url)).setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Bearer " + token)
                .build();

        // use the client to send the request
        CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        // We can do other things here while the request is in-flight

        // This blocks until the request is complete
        HttpResponse<String> response = responseFuture.get();

        return response.body();
    }


    @Override
    public void run() {
        try {
            // sync attribute, baseEntity, question
            log.info("Syncing attributes for realm:" + realm);
            String getUrl = GennySettings.qwandaServiceUrl + "/service/synchronize/cache/attributes";
            String response = sendGet(getUrl, authToken);
            logResponse(getUrl, response);

            log.info("Syncing baseentitys for realm:" + realm);
            getUrl = GennySettings.qwandaServiceUrl + "/service/synchronize/cache/baseentitys";
            response = sendGet(getUrl, authToken);
            logResponse(getUrl, response);

            log.info("Syncing questions for realm:" + realm);
            getUrl = GennySettings.qwandaServiceUrl + "/service/synchronize/cache/questions";
            response = sendGet(getUrl, authToken);
            logResponse(getUrl, response);
            log.info("Finished entities synchronization for realm:" + realm);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Exception:" + ex.getMessage() + " occurred when do sync entities for realm:" + realm);
        }
    }
}
