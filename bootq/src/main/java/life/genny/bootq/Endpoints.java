package life.genny.bootq;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import life.genny.bootq.models.BatchLoading;
import life.genny.bootq.sheets.Realm;
import life.genny.bootq.sheets.RealmUnit;
import life.genny.serviceq.Service;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/bootq/")
public class Endpoints {

    private boolean isBatchLoadingRunning = false;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authUrl;

	@Inject
    Logger log;

	@Inject
	Service service;

    @Inject
    JsonWebToken accessToken;

	@Inject
	BatchLoading bl;

    public boolean getIsTaskRunning() {
        return isBatchLoadingRunning;
    }

    public void setIsTaskRunning(boolean isTaskRunning) {
        this.isBatchLoadingRunning = isTaskRunning;
    }

	void onStart(@Observes StartupEvent ev) {
		service.showConfiguration();
		service.initToken();
		service.initCache();
		log.info("[*] Finished Startup!");
	}

    @Transactional
    void onShutdown(@Observes ShutdownEvent ev) {
        log.info("Bootq Endpoint Shutting down");
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        log.info("Application endpoint hit");
        return Response.status(200).entity("Application version:" + version).build();
    }

    @GET
    @Path("/loadsheets")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsUsingDefaultSheetId() {
        String defaultSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
        if (defaultSheetId != null) {
            return Response.ok().entity(loadSheetsById(defaultSheetId)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Can't find default google sheetId, please set environment variable GOOGLE_HOSTING_SHEET_ID, or call /loadsheets/{sheetid}")
                    .build();
        }
    }

    @GET
    @Path("/loadsheets/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsById(@PathParam("sheetid") final String sheetId) {
        log.info("Loading in sheet " + sheetId);
        String msg = "";
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }

        setIsTaskRunning(true);

        Realm realm = new Realm(sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        try {
            for (RealmUnit realmUnit : realmUnits) {
                log.info("Importing from sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());

                if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                    log.info("Persisting project...");
                    bl.persistProject(realmUnit);
                    log.info("Finished Persisting project");
                } else {
                    log.info("SKIPPING sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());
                }
                msg = "Finished batch loading for all realms in google sheets";
            }
        } catch (Exception ex) {
			ex.printStackTrace();
            msg = "Exception:" + ex.getMessage() + " occurred when batch loading";
        } finally {
            setIsTaskRunning(false);
        }
        log.info(msg);
        return Response.ok().entity(msg).build();
    }

    @GET
    @Path("/loadsheets/{sheetid}/{table}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsTableById(@PathParam("sheetid") final String sheetId, @PathParam("table") final String table) {
        log.info("Loading in sheet " + sheetId);
        String msg = "";
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }

        setIsTaskRunning(true);

        Realm realm = new Realm(sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        try {
            for (RealmUnit realmUnit : realmUnits) {
                log.info("Importing from sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());

                if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                    log.info("Persisting table " + table + "...");
                    bl.persistTable(realmUnit, table);
                    log.info("Finished Persisting table " + table);
                } else {
                    log.info("SKIPPING sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());
                }
                msg = "Finished batch loading for all realms in google sheets";
            }
        } catch (Exception ex) {
			ex.printStackTrace();
            msg = "Exception:" + ex.getMessage() + " occurred when batch loading";
        } finally {
            setIsTaskRunning(false);
        }
        log.info(msg);
        return Response.ok().entity(msg).build();
    }

}
