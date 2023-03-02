package life.genny.bootq.service;

import org.apache.logging.log4j.Logger;

import life.genny.bootq.models.SheetState;
import life.genny.bootq.sheets.module.ModuleUnit;
import life.genny.bootq.sheets.realm.RealmUnit;
import life.genny.bootq.utils.GoogleImportService;
import life.genny.bootq.utils.XlsxImport;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
			modules.add(moduleUnit);
        }

        return modules;
    }
}
