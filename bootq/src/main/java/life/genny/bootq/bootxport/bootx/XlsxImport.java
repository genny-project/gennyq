package life.genny.bootq.bootxport.bootx;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.tuples.Tuple2;

public abstract class XlsxImport {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    public abstract List<Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName);

    public abstract Map<String, Map<String, String>> mappingRawToHeaderAndValuesFmt(String sheetURI, String sheetName, Set<String> keys);

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
}
