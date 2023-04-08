package life.genny.bootq;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import life.genny.bootq.models.BatchLoading;
import life.genny.bootq.models.reporting.LoadReport;
import life.genny.bootq.sheets.realm.Realm;
import life.genny.bootq.sheets.realm.RealmUnit;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.serviceq.Service;

import java.sql.SQLException;
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/bootq/")
public class Endpoints {

    private boolean isBatchLoadingRunning = false;

    @ConfigProperty(name = "quarkus.application.version")
    String version;

	@Inject
    Logger log;

	@Inject
	Service service;

    @Inject
    UserToken userToken;

	@Inject
	BatchLoading bl;

    @Inject
    LoadReport loadReport;

    private static final boolean SHOW_STACK_TRACES = false;

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
        String defaultSheetId = CommonUtils.getSystemEnv("GOOGLE_HOSTING_SHEET_ID", "");
        if (StringUtils.isBlank(defaultSheetId)) {
            return loadSheetsById(defaultSheetId);
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
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        log.info("Loading in sheet " + sheetId);
        String msg = "";

        if (sheetId == null) {
            msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }

        setIsTaskRunning(true);
        Long start = System.currentTimeMillis();

        Realm realm = new Realm(sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        log.info("FOUND " + realmUnits.size() + " realmUnits");
        for (RealmUnit realmUnit : realmUnits) {
            log.info("Module Unit: " + realmUnit.getModule());
            log.info("Importing from sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());

            if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                log.infof("Persisting project for realmUnit code: %s", realmUnit.getCode());
                bl.persistProject(realmUnit);
                log.infof("Finished persisting project for realmUnit code: %s", realmUnit.getCode());
            } else {
                log.info("SKIPPING sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());
            }
            msg = "Finished batch loading for all realms in google sheets";
        }
        log.info(msg);
        setIsTaskRunning(false);
        Long end = System.currentTimeMillis();
        log.infof("Total time taken to load the sheet %s : %s (millis)", sheetId, (end - start));
        try {
            loadReport.printLoadReport(SHOW_STACK_TRACES);
        } catch(Exception e) {
            log.error("Error dumping to file");
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return Response.ok().entity(msg).build();
    }

    @GET
    @Path("/loadsheets/{sheetid}/{table}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsTableById(@PathParam("sheetid") final String sheetId, @PathParam("table") final String table) {
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        log.info("Loading in sheet " + sheetId);
        String msg = "";
        if (StringUtils.isBlank(sheetId)) {
            msg = "Sheet Id not supplied as path param!";
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
                    try {
                        bl.persistTable(realmUnit, table);
                    } catch (IllegalStateException e) {
                        // TODO: lint the table name ealier
                        log.error("Bad Table: " + table);
                        return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
                    }
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
        try {
            loadReport.printLoadReport(SHOW_STACK_TRACES);
        } catch(java.io.IOException e) {
            log.error("Error dumping to file");
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return Response.ok().entity(msg).build();
    }

    @GET
    @Path("/loadsheetstosqlite/{sheetid}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSheetsByIdToSqlite(@PathParam("sheetid") final String sheetId) {
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }

        if (sheetId == null) {
            String msg = "Can't find env GOOGLE_SHEETS_ID!!!";
            log.error(msg);
            setIsTaskRunning(false);
            return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
        }

        log.info("Loading in sheet " + sheetId + " to Sqlite");

        setIsTaskRunning(true);
        Long start = System.currentTimeMillis();

        Realm realm = new Realm(sheetId);
        List<RealmUnit> realmUnits = realm.getDataUnits();
        log.info("FOUND " + realmUnits.size() + " realmUnits");
        for (RealmUnit realmUnit : realmUnits) {
            log.info("Module Unit: " + realmUnit.getModule());
            log.info("Importing from sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());

            if (!realmUnit.getDisable() && !realmUnit.getSkipGoogleDoc()) {
                log.infof("Persisting project for realmUnit code: %s", realmUnit.getCode());
                try {
                    bl.loadToSqlite(realmUnit);
                } catch (Exception e) {
                    log.errorf("Can't load data from google sheets to sqlite: %s", e.getMessage());
                    e.printStackTrace();
                    setIsTaskRunning(false);
                    return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
                }
                log.infof("Finished loading data to Sqlite for realmUnit code: %s", realmUnit.getCode());
            } else {
                log.info("SKIPPING sheet " + realmUnit.getUri() + " for realm " + realmUnit.getName());
            }
        }
        String message = "Finished batch loading for all realms in google sheets";
        log.info(message);
        setIsTaskRunning(false);
        Long end = System.currentTimeMillis();
        log.infof("Total time taken to load the sheet %s : %s (millis)", sheetId, (end - start));
        try {
            loadReport.printLoadReport(SHOW_STACK_TRACES);
        } catch(Exception e) {
            log.error("Error dumping to file");
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return Response.ok().entity(message).build();
    }

    @GET
    @Path("/loadsqlitetodb/{realm}/{sqlitedbnames}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response loadSqliteToDB(@PathParam("realm") final String realm, @PathParam("sqlitedbnames") final String sqliteDbNames) {
        if (getIsTaskRunning()) {
            log.error("Batch loading task is running, please try later or force restart pod");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Batch loading task is running, please try later or force restart pod")
                    .build();
        }
        log.infof("Loading data in Sqlite to DB - realm: %s, databases: %s", realm, sqliteDbNames);
        setIsTaskRunning(true);
        Long start = System.currentTimeMillis();
        String[] sqliteDbNamesArr = StringUtils.split(sqliteDbNames, GennyConstants.COMMA);
        try {
            for (int i = 0; i < sqliteDbNamesArr.length; i++) {
                log.infof("Loading data in Sqlite database: %s", sqliteDbNamesArr[i]);
                bl.loadDataInSqliteToDB(realm, sqliteDbNamesArr[i]);
            }
        } catch (Exception e) {
            log.errorf("Error loading the data in sqlite to database: %s", e.getMessage());
            e.printStackTrace();
            setIsTaskRunning(false);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        String message = "Finished batch loading for all realms in google sheets";
        log.info(message);
        setIsTaskRunning(false);
        Long end = System.currentTimeMillis();
        log.infof("Total time taken to load from Sqlite : %s (millis)", (end - start));
        try {
            loadReport.printLoadReport(SHOW_STACK_TRACES);
        } catch(Exception e) {
            log.error("Error dumping to file");
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return Response.ok().entity(message).build();
    }

}
