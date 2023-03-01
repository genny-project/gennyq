package life.genny.bootq.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import life.genny.bootq.sheets.ESheetConfiguration;

import org.apache.logging.log4j.Logger;

public class XlsxImport {
    private final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private static final String RANGE = "!A1:Z";

    private Sheets service;

    public XlsxImport(Sheets service) {
        this.service = service;
    }

    public List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName) {
        ESheetConfiguration sheetConfiguration = ESheetConfiguration.getByTitle(sheetName);
        return getRows(sheetURI, sheetConfiguration);
    }

    /**
     * Take the raw data and convert it to header and rows (Each row is a List of Strings)
     * @param data
     * @return
     */
    public Tuple2<List<String>, List<List<String>>> sliceDataToHeaderAndValues(List<List<Object>> rawData) {
        if(rawData.isEmpty()) {
            log.error("Data to be sliced and Diced to HEader and Values is empty");
        }

        Tuple2<List<String>, List<List<String>>> headerAndValues = null;
        List<List<String>> data = rawData.stream()
        // For each row, map from object to string
        .map(row -> {
            List<String> stringifiedRow = new ArrayList<>(row.size());

            // Traditional for loop as the row collection is small
            for(Object cell : row) {
                stringifiedRow.add(cell.toString());
            }

            return stringifiedRow;
        }).collect(Collectors.toList());

        List<String> header = data.get(0).stream()
            .map(cell -> cell.toLowerCase().replaceAll("^\"|\"$|_|-", ""))
            .collect(Collectors.toList());
        
        data.remove(0);

        headerAndValues = Tuple.of(header, data);

        return headerAndValues;
    }
    
    /**
     * Get a list of rows (represented as Map<String, String>) from a raw List of List<Object>s where the first entry is always the header
     * row 
     * @param values - values to parse
     * @return - list of rows
     */
    public List<Map<String, String>> mappingHeaderToValues(final List<List<Object>> values) {
        final List<Map<String, String>> k = new ArrayList<>();
        Tuple2<List<String>, List<List<String>>> headerAndValues = sliceDataToHeaderAndValues(values);

        List<String> header = headerAndValues._1;
        List<List<String>> rows = headerAndValues._2;

        // Stitch the data together using the headers
        for (final List<String> row : rows) {
            final Map<String, String> mapper = new HashMap<>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(header.get(counter), row.get(counter).toString());
            }
            k.add(mapper);
        }
        return k;
    }

    /**
     * Get the rows from SpreadSheet for a particular sheet within a specified doc
     * @param sheetId - Id of the Google Doc to grab rows from a particular sheet for
     * @param sheetConfiguration - an {@link ESheetConfiguration SheetConfiguration} containing the name and header configuration 
     * @return a list of rows, where each row is represented as a Map<String, String> (keys as header names, values as values of cells)
     */
    private List<Map<String, String>> getRows(String sheetId, ESheetConfiguration sheetConfiguration) {
        String sheetName = sheetConfiguration.getTitle();

        List<List<Object>> data;
        try {
            data = fetchSpreadSheet(sheetId, sheetName);
            data = new ArrayList<>(data);
        } catch (IOException e) {
            log.error("Function2: There was a Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetId);
            return new ArrayList<>();
        }
        return mappingHeaderToValues(data);
    }

    public Map<String, Map<String, String>> mappingKeyHeaderToHeaderValues(
            final List<List<Object>> values, Set<String> sheetHeaders) {

        final Map<String, Map<String, String>> k = new HashMap<>();

        Tuple2<List<String>, List<List<String>>> headerAndValues = sliceDataToHeaderAndValues(values);

        final List<List<String>> rows = headerAndValues._2;

        // Stitch the data together
        for (final List<String> rawRow : rows) {
            final Map<String, String> row = new HashMap<>();

            for (int counter = 0; counter < rawRow.size(); counter++) {
                row.put(headerAndValues._1.get(counter), rawRow.get(counter).toString());
            }

            String join = row.keySet().stream()
                    .filter(sheetHeaders::contains).map(row::get).collect(Collectors.joining());
            
            log.info("Adding " + join + " to k (whatever this map is)");
            k.put(join, row);
        }
        return k;
    }

    public List<List<Object>> fetchSpreadSheet(String sheetId, String sheetName) throws IOException {
        final String absoluteRange = sheetName + RANGE;
        ValueRange response = service.spreadsheets().values().get(sheetId, absoluteRange).execute();
        return response.getValues();
        
    }
}
