package life.genny.bootq.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

@ApplicationScoped
public class BootSecurity {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

	// public static final String GOOGLE_GREDENTIALS_PATH = "/root/google_credentials/token-secret-service-account.json";
	public static final String GOOGLE_GREDENTIALS_PATH = "/root/google_credentials/credentials.json";
	public static final String APPLICATION_NAME = "Bootq";

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

	public NetHttpTransport transport;
	public Sheets sheetsService;

    public void init() {

		try {
			transport = GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
		try {
			this.sheetsService = new Sheets.Builder(transport, JSON_FACTORY, authorize(transport))
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public static Credential authorize(NetHttpTransport httpTransport) throws FileNotFoundException, IOException {
		File file = new File(GOOGLE_GREDENTIALS_PATH);
		if (!file.exists()) {
			throw new FileNotFoundException("Resource not found: " + GOOGLE_GREDENTIALS_PATH);
		}
		// Load client secrets.
		InputStream in = new FileInputStream(file);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		log.info(clientSecrets.getDetails().toPrettyString());

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
			httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
		.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
		.setAccessType("offline")
		.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

}
