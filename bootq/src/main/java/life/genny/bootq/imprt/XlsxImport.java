package life.genny.bootq.imprt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import io.smallrye.mutiny.tuples.Tuple2;

@ApplicationScoped
public class XlsxImport {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private static final String RANGE = "!A1:Z";

    private Sheets service;

    public XlsxImport() {
    }

    public Tuple2<List<String>, List<List<Object>>> sliceDataToHeaderAndValues(List<List<Object>> data) {
        Tuple2<List<String>, List<List<Object>>> headerAndValues = null;
        if (!data.isEmpty()) {
            List<String> header = data.get(0).stream()
                    .map(d -> d.toString().toLowerCase().replaceAll("^\"|\"$|_|-", ""))
                    .collect(Collectors.toList());
            data.remove(0);
            headerAndValues = Tuple2.of(header, data);
        } else {
            log.error("Data to be sliced and Diced to HEader and Values is empty");
        }

        return headerAndValues;
    }

    private List<Map<String, String>> mappingAndCacheHeaderToValues(String sheetURI, String sheetName) {

		log.info("SheetID: " + sheetURI + ", sheetName: " + sheetName);

		List<List<Object>> data;
		try {
			data = List.copyOf(fetchSpreadSheet(sheetURI, sheetName));
		} catch (IOException e) {
			log.error("Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetURI);
			return new ArrayList<>();
		}
		return mappingHeaderToValues(data);
	};

    private Map<String, Map<String, String>> mappingAndCacheKeyHeaderToHeaderValues(String sheetURI, String sheetName, String[] keys) {
		List<List<Object>> data;
		try {
			data = List.copyOf(fetchSpreadSheet(sheetURI, sheetName));
			log.info("sheetID:" + sheetURI + ", SheetName:" + sheetName + ", Value size:" + data.size());
		} catch (IOException e) {
			log.error("Error " + e.getMessage() + " in SheetName:" + sheetName + " and SheetID:" + sheetURI);
			return new HashMap<>();
		}
		return mappingKeyHeaderToHeaderValues(data, keys);
	};

    public List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName) {
        return mappingAndCacheHeaderToValues(sheetURI, sheetName);
    }

    public Map<String, Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName, String[] keys) {
        return mappingAndCacheKeyHeaderToHeaderValues(sheetURI, sheetName, keys);
    }

    public List<Map<String, String>> mappingHeaderToValues(
            final List<List<Object>> values) {
        final List<Map<String, String>> k = new ArrayList<>();
        Tuple2<List<String>, List<List<Object>>> headerAndValues = sliceDataToHeaderAndValues(values);
        for (final List<Object> row : headerAndValues.getItem2()) {
            final Map<String, String> mapper = new HashMap<>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(headerAndValues.getItem1().get(counter), row.get(counter).toString());
            }
            k.add(mapper);
        }
        return k;
    }

    public Map<String, Map<String, String>> mappingKeyHeaderToHeaderValues(
            final List<List<Object>> values, String[] keys) {
        final Map<String, Map<String, String>> k = new HashMap<>();
        Tuple2<List<String>, List<List<Object>>> headerAndValues = sliceDataToHeaderAndValues(values);
        for (final List<Object> row : headerAndValues.getItem2()) {
            final Map<String, String> mapper = new HashMap<>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(headerAndValues.getItem2().get(counter).toString(), row.get(counter).toString());
            }
            String join = mapper.keySet().stream()
					.filter(Arrays.asList(keys)::contains)
					.map(mapper::get)
					.collect(Collectors.joining());
            k.put(join, mapper);
        }
        return k;
    }

    public List<List<Object>> fetchSpreadSheet(String sheetId, String sheetName) throws IOException {
        final String absoluteRange = sheetName + RANGE;
        ValueRange response = service.spreadsheets().values().get(sheetId, absoluteRange).execute();
        return response.getValues();
        
    }

}
