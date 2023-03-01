package life.genny.bootq.utils.xlsx;

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
import com.google.common.collect.Lists;
import io.vavr.Function2;
import io.vavr.Function3;
import io.vavr.Tuple2;
import org.apache.logging.log4j.Logger;

public class XlsxImportOnline extends XlsxImport {
    private final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private static final String RANGE = "!A1:Z";

    private Sheets service;

    private Function2<String, String, List<Map<String, String>>> mappingAndCacheHeaderToValues =
            (sheetURI, sheetName) -> {
                log.info("Function2: not memoized for SheetID: " + sheetURI + ",sheetName: " + sheetName);
                List<List<Object>> data;
                try {
                    data = Lists.newArrayList(fetchSpreadSheet(sheetURI, sheetName));
                } catch (IOException e) {
                    log.error("Function2: There was a Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetURI);
                    return new ArrayList<>();
                }
                return mappingHeaderToValues(data);
            };

    private Function3<String, String, Set<String>, Map<String, Map<String, String>>> mappingAndCacheKeyHeaderToHeaderValues =
            (sheetURI, sheetName, keys) -> {
                List<List<Object>> data;
                try {
                    data = Lists.newArrayList(fetchSpreadSheet(sheetURI, sheetName));
                    log.info("Function3: not memoized for sheetID:" + sheetURI + ", SheetName:" + sheetName + ", Value size:" + data.size());
//                    System.out.println("sheetID:" + sheetURI + ", SheetName:" + sheetName + ", Value size:" + data.size());

                } catch (IOException e) {
                    log.error("Function3: There was a Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetURI);
//                    System.out.println("Function3: There was a Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetURI);
                    return new HashMap<>();
                }
                return mappingKeyHeaderToHeaderValues(data, keys);
            };

    public XlsxImportOnline(Sheets service) {
        this.service = service;
        memoized();
    }

    @Override
    public List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName) {
        return mappingAndCacheHeaderToValues.apply(sheetURI, sheetName);
    }

    @Override
    public Map<String, Map<String, String>> mappingRawToHeaderAndValuesFmt(
            String sheetURI, String sheetName, Set<String> keys) {
        return mappingAndCacheKeyHeaderToHeaderValues.apply(sheetURI, sheetName, keys);
    }

    public List<Map<String, String>> mappingHeaderToValues(
            final List<List<Object>> values) {
        final List<Map<String, String>> k = new ArrayList<>();
        Tuple2<List<String>, List<List<Object>>> headerAndValues = sliceDataToHeaderAndValues(values);
        for (final List<Object> row : headerAndValues._2) {
            final Map<String, String> mapper = new HashMap<>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(headerAndValues._1.get(counter), row.get(counter).toString());
            }
            k.add(mapper);
        }
        return k;
    }

    public Map<String, Map<String, String>> mappingKeyHeaderToHeaderValues(
            final List<List<Object>> values, Set<String> keyColumns) {
        final Map<String, Map<String, String>> k = new HashMap<>();
        Tuple2<List<String>, List<List<Object>>> headerAndValues = sliceDataToHeaderAndValues(values);
        for (final List<Object> row : headerAndValues._2) {
            final Map<String, String> mapper = new HashMap<>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(headerAndValues._1.get(counter), row.get(counter).toString());
            }
            String join = mapper.keySet().stream()
                    .filter(keyColumns::contains).map(mapper::get).collect(Collectors.joining());
            k.put(join, mapper);
        }
        return k;
    }

    public List<List<Object>> fetchSpreadSheet(String sheetId, String sheetName) throws IOException {
        final String absoluteRange = sheetName + RANGE;
        ValueRange response = service.spreadsheets().values().get(sheetId, absoluteRange).execute();
        return response.getValues();
        
    }


    Map<String, List<List<Object>>> responseState = new HashMap<>();

    public void memoized() {
        mappingAndCacheHeaderToValues = mappingAndCacheHeaderToValues.memoized();
        mappingAndCacheKeyHeaderToHeaderValues = mappingAndCacheKeyHeaderToHeaderValues.memoized();
    }
}
