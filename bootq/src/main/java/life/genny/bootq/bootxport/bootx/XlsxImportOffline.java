package life.genny.bootq.bootxport.bootx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.vavr.Function2;
import io.vavr.Function3;
import io.vavr.Tuple2;

public class XlsxImportOffline extends XlsxImport {

    private XSSFService service;

    public XlsxImportOffline(XSSFService service) {
        this.service = service;
        memoized();
    }

    public <T> Stream<T> fromIteratorToStream(Iterator<T> iterator) {

        Iterable<T> iterable = () -> iterator;

        Stream<T> targetStream =
                StreamSupport.stream(iterable.spliterator(), false);

        return targetStream;
    }

    private Function2<String, String, List<Map<String, String>>> mappingAndCacheHeaderToValues =
            (sheetId, sheetName) -> {
                List<List<Object>> data = service.offlineService(sheetId, sheetName);
                return mappingHeaderToValues(data);
            };

    private Function3<String, String, Set<String>, Map<String, Map<String, String>>> mappingAndCacheKeyHeaderToHeaderValues =
            (sheetId, sheetName, keys) -> {
                List<List<Object>> data = service.offlineService(sheetId, sheetName);
                return mappingKeyHeaderToHeaderValues(data, keys);
            };

    public List<Map<String, String>> mappingHeaderToValues(
            final List<List<Object>> values) {
        final List<Map<String, String>> k = new ArrayList<>();
        Tuple2<List<String>, List<List<Object>>> headerAndValues = sliceDataToHeaderAndValues(values);
        for (final List<Object> row : headerAndValues._2) {
            final Map<String, String> mapper = new HashMap<String, String>();
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
            final Map<String, String> mapper = new HashMap<String, String>();
            for (int counter = 0; counter < row.size(); counter++) {
                mapper.put(headerAndValues._1.get(counter), row.get(counter).toString());
            }
            String join = mapper.keySet().stream()
                    .filter(keyColumns::contains).map(mapper::get).collect(Collectors.joining());
            k.put(join, mapper);
        }
        return k;
    }

    @Override
    public List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName) {
        long timeBefore, timeAfter = 0;
        timeBefore = System.currentTimeMillis();
        List<Map<String, String>> result = mappingAndCacheHeaderToValues.apply(sheetURI, sheetName);
        timeAfter = System.currentTimeMillis();
        System.out.println("In sheet: " + sheetURI + " " + sheetName);
        return result;
    }

    @Override
    public Map<String, Map<String, String>> mappingRawToHeaderAndValuesFmt(
            String sheetURI, String sheetName, Set<String> keys) {
        Map<String, Map<String, String>> result = new HashMap<>();
        long timeBefore, timeAfter = 0;
        timeBefore = System.currentTimeMillis();
        result = mappingAndCacheKeyHeaderToHeaderValues.apply(sheetURI, sheetName, keys);
        if (sheetName.equals("EntityAttribute"))
            System.out.println(result);
        System.out.println(result.size());
        timeAfter = System.currentTimeMillis();
        System.out.println("In sheet: " + sheetURI + " " + sheetName);
        return result;
    }

    public void memoized() {
        mappingAndCacheHeaderToValues = mappingAndCacheHeaderToValues.memoized();
        mappingAndCacheKeyHeaderToHeaderValues = mappingAndCacheKeyHeaderToHeaderValues.memoized();
    }

}
