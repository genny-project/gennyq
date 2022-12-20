package life.genny.bootq.bootxport.bootx;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.bootq.utils.GennySheets;

import static java.lang.Thread.sleep;

/**
 * @author acrow
 */
public class ProjectsLoading {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    public ProjectsLoading() {
    }

    public static Map<String, Map> loadIntoMap(final String hostSheetId, final String secret, File credentialPath) throws InterruptedException {
        GennySheets sheets = new GennySheets(secret, hostSheetId, credentialPath);

        List<Map> dataMaps = null;
        Integer countDown = 10;
        while (countDown > 0) {
            try {
                dataMaps = sheets.hostingImport();
                break;
            } catch (Exception ee) {
                log.error("Load from Google Doc failed, trying again in 3 sec");
                sleep(3000);
                countDown--;
            }
        }

        Map<String, Map> returnMap = new HashMap<String, Map>();
        for (Map data : dataMaps) {
            String code = (String) data.get("code");
            if (!StringUtils.isBlank(code)) {
                returnMap.put(code, data);
            }
        }
        return returnMap;
    }


    private static Boolean getBooleanFromString(final String booleanString) {
        if (booleanString == null) {
            return false;
        }

        return "TRUE".equalsIgnoreCase(booleanString.toUpperCase())
                || "YES".equalsIgnoreCase(booleanString.toUpperCase())
                || "T".equalsIgnoreCase(booleanString.toUpperCase())
                || "Y".equalsIgnoreCase(booleanString.toUpperCase())
                || "1".equalsIgnoreCase(booleanString);
    }
}
