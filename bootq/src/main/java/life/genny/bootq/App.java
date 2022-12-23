package life.genny.bootq;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import life.genny.bootq.bootxport.bootx.Realm;
import life.genny.bootq.bootxport.bootx.RealmUnit;
import life.genny.bootq.bootxport.xlsimport.BatchLoading;

@Path("/bootq/")
public class App {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private boolean isBatchLoadingRunning = false;

    @Inject
    BatchLoading bl;

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsUsingDefaultSheetId() {
        String defaultSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
        if (defaultSheetId == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
		return loadSheetsById(defaultSheetId);
    }


    @GET
    @Path("/loadsheets/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsById(@PathParam("sheetid") final String sheetId) {
        log.info("Loading in sheet " + sheetId);
        if (isBatchLoadingRunning) {
            log.error("Batch loading task is running, please try later.");
            return Response.status(Status.CONFLICT).build();
        }

        if (sheetId == null) {
            log.error("Can't find env GOOGLE_SHEETS_ID!!!");
            return Response.status(Status.NOT_FOUND).build();
        }

        isBatchLoadingRunning = true;

        Realm realm = new Realm(sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();

		for (RealmUnit realmUnit : realmUnits) {
			if (realmUnit.getDisable() || realmUnit.getSkipGoogleDoc()) {
				continue;
			}
			bl.persistProjectOptimization(realmUnit);
			log.info("Finished loading sheet:" + realmUnit.getUri() + ", realm:" + realmUnit.getName());
		}
		log.info("Finished batch loading for all realms in google sheets");
        return Response.ok().build();
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        log.info("Bootq Endpoint starting");

    }

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Bootq Endpoint Shutting down");
    }
}
