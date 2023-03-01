package life.genny.bootq.models;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import io.vavr.Tuple;
import io.vavr.Tuple2;

public abstract class XlsxImport {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    public abstract List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName);

    public abstract Map<String, Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName, Set<String> keys);

    public Tuple2<List<String>, List<List<Object>>> sliceDataToHeaderAndValues(List<List<Object>> data) {
        Tuple2<List<String>, List<List<Object>>> headerAndValues = null;
        if (!data.isEmpty()) {
            List<String> header = data.get(0).stream()
                    .map(d -> d.toString().toLowerCase().replaceAll("^\"|\"$|_|-", ""))
//				.peek(System.out::println)
                    .collect(Collectors.toList());
            data.remove(0);
            headerAndValues = Tuple.of(header, data);
        } else {
            log.error("Data to be sliced and Diced to HEader and Values is empty");
        }

        return headerAndValues;
    }
}
