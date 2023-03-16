package life.genny.bootq.sheets.module;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import life.genny.bootq.sheets.DataUnit;
import life.genny.bootq.sheets.ESheetConfiguration;
import life.genny.bootq.utils.GoogleImportService;
import life.genny.bootq.utils.XlsxImport;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A POJO representation of a Google Doc and 
 */
public class ModuleUnit extends DataUnit {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    
    private static final String RANGE = "!A1:Z";

    public ModuleUnit() {
        super();
    }

    public void init(String sheetURI) {
        Sheets sheetsService = GoogleImportService.getInstance().getService();

        ArrayList<Sheet> sheets = getSheets(sheetsService,sheetURI);
        // get tiles
        Set<String> titles = new HashSet<>();
        for (Sheet sheet : sheets) {
            SheetProperties sheetProperties = (SheetProperties)sheet.get("properties");
            String title = sheetProperties.getTitle();
            if (ESheetConfiguration.isValidTitle(title))
                titles.add(title);
        }

        ArrayList<ValueRange> valueRanges = getValueRanges(sheetsService, sheetURI, titles);

        processValues(sheetsService, titles, valueRanges, sheetURI);

    }

    // Get all sheets from spreadSheet
    private ArrayList<Sheet> getSheets(Sheets service, String spreadsheetId) {
        // The ranges to retrieve from the spreadsheet.
        List<String> ranges = new ArrayList<>();
        ArrayList<Sheet> sheets = new ArrayList<>();

        // True if grid data should be returned.
        // This parameter is ignored if a field mask was set in the request.
        boolean includeGridData = false;
        try {
            Sheets.Spreadsheets.Get request = service.spreadsheets().get(spreadsheetId);
            request.setRanges(ranges);
            request.setIncludeGridData(includeGridData);

            Spreadsheet response = request.execute();
            sheets = (ArrayList<Sheet>) response.get("sheets");
        } catch (IOException ioe) {
            log.error("IOException occurred when fetch SpreadSheets:" + spreadsheetId);
        }
        return sheets;
    }

    private ArrayList<ValueRange> getValueRanges(Sheets service, String spreadsheetId, Set<String> titles) {
        // The ranges to retrieve from the spreadsheet.
        List<String> ranges = new ArrayList<>();
        ArrayList<ValueRange> valueRanges = new ArrayList<>();

        for (String title: titles) {
            ranges.add(title + RANGE);
        }

        // True if grid data should be returned.
        // This parameter is ignored if a field mask was set in the request.
        try {
            Sheets.Spreadsheets.Values.BatchGet request = service.spreadsheets().values().batchGet(spreadsheetId);
            request.setRanges(ranges);
            BatchGetValuesResponse response = request.execute();
            valueRanges = (ArrayList<ValueRange>) response.get("valueRanges");
        } catch (IOException ioe) {
            log.error("IOException occurred when fetch SpreadSheets:" + spreadsheetId + ", exception msg:" + ioe.getMessage());
        }
        return valueRanges;
    }

    /**
     * Get all rows for a sheet within this Google Doc/Module
     * @param sheetsService - {@link Sheets Google Sheets Service}
     * @param sheetConfiguration - sheet metadata including the title of the sheet to target
     * @param values - the values to map
     * @param sheetId - the id of the google doc to import from 
     * @return
     */
    private Map<String, Map<String, String>> parseRows(Sheets sheetsService, ESheetConfiguration sheetConfiguration,
    List<List<Object>> values, String sheetId) {
        XlsxImport xlsxImportOnline = new XlsxImport(sheetsService);
        Map<String, Map<String, String>> tmp =  new HashMap<>();

        try {
            tmp = xlsxImportOnline.mappingKeyHeaderToHeaderValues(values, sheetConfiguration.getHeaderRow());
        } catch (Exception ex) {
            logFetchExceptionForSheets(ex.getMessage(), sheetConfiguration.getTitle(), sheetId);
        }
        return tmp;
    }

    private void processValues (Sheets sheetsService, Set<String> titles, ArrayList<ValueRange> valueRanges,
                                String sheetURI) {
    	if (valueRanges == null) {
    		return;
    	}

        log.info("Processing Module: " + this.name);

        for (ValueRange valueRange : valueRanges) {
            String title = valueRange.getRange().split("!")[0];

            if (titles.contains(title)) {
                ESheetConfiguration titleData = ESheetConfiguration.getByTitle(title);

                List<List<Object>> values = valueRange.getValues();
                log.info("\tprocessing " + titleData.name() + ", value size:" + values.size());
                
                switch (titleData) {
                    case VALIDATION:
                        this.validations = parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case DATATYPE:
                        this.dataTypes= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case ATTRIBUTE:
                        this.attributes= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case BASE_ENTITY:
                        this.baseEntitys= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case QUESTION_QUESTION:
                        this.questionQuestions= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case QUESTION:
                        this.questions= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case ENTITY_ATTRIBUTE:
                        this.entityAttributes= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case DEF_BASE_ENTITY:
                        this.def_baseEntitys= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    case DEF_ENTITY_ATTRIBUTE:
                        this.def_entityAttributes= parseRows(sheetsService, titleData, values, sheetURI);
                        break;
                    default:
                        log.error("Unhandled ESheetTitle!: " + title);
                        break;
                }
            }
        }
    }

    private void logFetchExceptionForSheets(String exception, String sheetName, String sheetURI) {
        log.error("ATTENTION!, Exception:"  +  exception + " occurred when fetching sheetName:" + sheetName
                + ", sheetURI:" + sheetURI + ", return EMPTY HashMap!!!");
    }
}
