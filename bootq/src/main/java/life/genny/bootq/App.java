package life.genny.bootq;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import life.genny.bootq.bootxport.bootx.BatchLoadMode;
import life.genny.bootq.bootxport.bootx.QwandaRepository;
import life.genny.bootq.bootxport.bootx.Realm;
import life.genny.bootq.bootxport.bootx.RealmUnit;
import life.genny.bootq.bootxport.xlsimport.BatchLoading;
import life.genny.bootq.utils.SyncEntityThread;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@Path("/bootq/")
public class App {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private boolean isBatchLoadingRunning = false;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authUrl;


    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        return Response.status(200).entity("Application version:" + version).build();
    }

    @Inject
    EntityManager em;

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String loadSheetsUsingDefaultSheetId() {
        String defaultSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
        if (defaultSheetId != null) {
            return loadSheetsById(defaultSheetId);
        } else {
            return "Can't find default google sheetId, please set environment variable GOOGLE_HOSTING_SHEET_ID, or call /loadsheets/{sheetid}";
        }
    }


    @GET
    @Path("/loadsheets/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String loadSheetsById(@PathParam("sheetid") final String sheetId) {
        log.info("Loading in sheet " + sheetId);
        String msg = "";
        String authToken = accessToken.getRawToken();

        if (isBatchLoadingRunning) {
            return "Batch loading task is running, please try later.";
        }

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return msg;
        }

        isBatchLoadingRunning = true;

        Realm realm = new Realm(BatchLoadMode.ONLINE, sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        try {
            for (RealmUnit realmUnit : realmUnits) {
                if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                    QwandaRepository repo = new QwandaRepositoryService(em);
                    BatchLoading bl = new BatchLoading(repo);
                    bl.persistProjectOptimization(realmUnit);
                    log.info("Finished batch loading for sheet:" + realmUnit.getUri()
                            + ", realm:" + realmUnit.getName() + ", now syncing be, attr and questions");

                    SyncEntityThread syncEntityThread = new SyncEntityThread(authToken, realmUnit.getName());
                    syncEntityThread.start();
                }
                msg = "Finished batch loading for all realms in google sheets";
            }
        } catch (Exception ex) {
            msg = "Exception:" + ex.getMessage() + " occurred when batch loading";
        } finally {
            setIsTaskRunning(false);
        }
        log.info(msg);
        return msg;
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Bootq Endpoint starting with auth Server " + authUrl);

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Bootq Endpoint Shutting down");
    }
}
