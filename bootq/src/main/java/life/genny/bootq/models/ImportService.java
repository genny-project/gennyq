package life.genny.bootq.models;

import org.apache.logging.log4j.Logger;

import life.genny.bootq.sheets.ESheetTitle;
import life.genny.bootq.sheets.RealmUnit;
import life.genny.bootq.sheets.module.ModuleUnit;
import life.genny.bootq.utils.GoogleImportService;
import life.genny.bootq.utils.XlsxImport;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportService {
    private final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
    private Map<String, XlsxImport> state;

    public XlsxImport createXlsImport(String key) {
        if (SheetState.getUpdateState().contains(key)) {
            XlsxImport xlsxImportOnline = new XlsxImport(GoogleImportService.getInstance().getService());
            state.put(key, xlsxImportOnline);
            SheetState.removeUpdateState(key);
            log.info("The state it is being updated... " + key);
            return xlsxImportOnline;
        }
        if (state.containsKey(key)) {
            return state.get(key);
		}

		log.info("Creating a new Import service for " + key);
		XlsxImport xlsxImportOnline = new XlsxImport(GoogleImportService.getInstance().getService());
		state.put(key, xlsxImportOnline);
		return xlsxImportOnline;
    }

    public ImportService(Map<String, XlsxImport> state) {
        this.state = state;
    }

    public List<RealmUnit> fetchRealmUnit(String sheetURI) {
        String projects = "Projects";
        String key = sheetURI + projects;
        
        XlsxImport createXlsImport = createXlsImport(key);
        List<RealmUnit> list = new ArrayList<>();
        for (Map<String, String> rawData : createXlsImport
                .mappingRawToHeaderAndValuesFmt(sheetURI, projects)) {
            if (!rawData.isEmpty()) {
                RealmUnit name = new RealmUnit(rawData);
                list.add(name);
            }
        }
        return list;
    }

    public List<ModuleUnit> fetchModuleUnit(String sheetURI) {
        String key = sheetURI + "Modules";
        XlsxImport createXlsImport = createXlsImport(key);
        List<Map<String, String>> data = createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, "Modules");
        List<ModuleUnit> modules = new ArrayList<>();

        for(Map<String, String> module : data) {
            if(module.isEmpty())
                continue;
            ModuleUnit moduleUnit = new ModuleUnit(module.get("sheetid"));
            moduleUnit.setName(module.get("name"));
        }

        return modules;
    }

    public Map<String, Map<String, String>> fetchEntitiesFromSheet(String sheetURI, ESheetTitle sheetData) {
        String key = sheetURI + sheetData.getTitle();
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, sheetData.getTitle(), sheetData.getDataKeyColumns());
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), sheetData.getTitle(), sheetURI);
            return new HashMap<>();
        }
    }

    private void logFetchExceptionForSheets(String exception, String sheetName, String sheetURI) {
        log.error("ATTENTION!, Exception:"  +  exception + " occurred when fetching sheetName:" + sheetName
        + ", sheetURI:" + sheetURI + ", return EMPTY HashMap!!!");
    }
}
