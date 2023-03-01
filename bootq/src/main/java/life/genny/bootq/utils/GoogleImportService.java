package life.genny.bootq.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.apache.logging.log4j.Logger;

public class GoogleImportService {

    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    private final JsonFactory jacksonFactory = JacksonFactory.getDefaultInstance();

    private HttpTransport httpTransport;

    private final List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private Sheets service;

    public Sheets getService() {
        return service;
    }

    private static GoogleImportService instance = null;

    public static GoogleImportService getInstance() {
        synchronized (GoogleImportService.class) {
            if (instance == null) {
                instance = new GoogleImportService();
            }
        }
        return instance;
    }

    private GoogleImportService() {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            service = getSheetsService();
        } catch (final IOException e) {
            log.error(e.getMessage());
        } catch (final Exception ex) {
            log.error(ex.getMessage());
            System.exit(1);
        }
    }


    public Sheets getSheetsService() throws IOException {
        final Credential credential = authorize();
        return new Sheets.Builder(httpTransport, jacksonFactory,
                credential).build();
    }

    public Credential authorize() throws IOException {
        Optional<String> path = Optional.ofNullable(System.getenv("GOOGLE_SVC_ACC_PATH"));
        if (!path.isPresent()) {
            throw new FileNotFoundException("GOOGLE_SVC_ACC_PATH not set");
        }

        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(path.get()),
                httpTransport, jacksonFactory).createScoped(scopes);

        if (credential != null) {
            String msg = String.format("Spreadsheets being read with user id: %s.", credential.getServiceAccountId());
            log.info(msg);
            log.info(credential.getTokenServerEncodedUrl());
        }
        return credential;
    }

}
