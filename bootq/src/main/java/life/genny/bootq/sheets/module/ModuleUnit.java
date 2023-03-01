package life.genny.bootq.sheets.module;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import life.genny.bootq.sheets.DataUnit;
import life.genny.bootq.sheets.ESheetTitle;
import life.genny.bootq.utils.xlsx.XlsxImportOnline;
import life.genny.bootq.utils.GoogleImportService;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleUnit extends DataUnit {
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    
    private static final String RANGE = "!A1:Z";

    public ModuleUnit(String sheetURI) {
        log.info("Processing spreadsheet:" + sheetURI);
        Sheets sheetsService = GoogleImportService.getInstance().getService();
        ArrayList<Sheet> sheets = getSheets(sheetsService,sheetURI);

        // get tiles
        Set<String> titles = new HashSet<>();
        for (Sheet sheet : sheets) {
            SheetProperties sheetProperties = (SheetProperties)sheet.get("properties");
            String title = sheetProperties.getTitle();
            if (ESheetTitle.isValidTitle(title))
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

    private Map<String, Map<String, String>> getData(Sheets sheetsService, ESheetTitle titleData,
    List<List<Object>> values, String sheetURI) {
        XlsxImportOnline xlsxImportOnline = new XlsxImportOnline(sheetsService);
        Map<String, Map<String, String>> tmp =  new HashMap<>();

        try {
            tmp = xlsxImportOnline.mappingKeyHeaderToHeaderValues(values, titleData.getDataKeyColumns());
        } catch (Exception ex) {
            logFetchExceptionForSheets(ex.getMessage(), titleData.getTitle(), sheetURI);
        }
        return tmp;
    }

    private void processValues (Sheets sheetsService, Set<String> titles, ArrayList<ValueRange> valueRanges,
                                String sheetURI) {
    	if (valueRanges == null) {
    		return;
    	}
        for (ValueRange valueRange : valueRanges) {
            String title = valueRange.getRange().split("!")[0];

            if (titles.contains(title)) {
                ESheetTitle titleData = ESheetTitle.getByTitle(title);

                List<List<Object>> values = valueRange.getValues();
                log.info("processing " + title + ", value size:" + values.size());
                switch (titleData) {
                    case VALIDATION:
                        this.validations = getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case DATATYPE:
                        this.dataTypes= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case ATTRIBUTE:
                        this.attributes= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case BASE_ENTITY:
                        this.baseEntitys= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case QUESTION_QUESTION:
                        this.questionQuestions= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case QUESTION:
                        this.questions= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case ENTITY_ATTRIBUTE:
                        this.entityAttributes= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case DEF_BASE_ENTITY:
                        this.def_baseEntitys= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    case DEF_ENTITY_ATTRIBUTE:
                        this.def_entityAttributes= getData(sheetsService, titleData, values, sheetURI);
                        break;
                    default:
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
