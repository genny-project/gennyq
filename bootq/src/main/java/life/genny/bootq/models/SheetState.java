package life.genny.bootq.models;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import life.genny.bootq.sheets.Realm;
import life.genny.bootq.sheets.RealmUnit;
import life.genny.bootq.utils.xlsx.XlsxImport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SheetState {
    private static final Log log = LogFactory.getLog(SheetState.class);

    private SheetState() {
    }

    private static Map<String, XlsxImport> state = new HashMap<>();
    private static Set<String> updateState = new HashSet<>();
    public static Map<String, RealmUnit> previousRealmUnit = new HashMap<>();
    public static Realm previousRealm;

    public static Realm getPreviousRealm() {
        return previousRealm;
    }

    public static void setPreviousRealm(Realm previousRealm) {
        SheetState.previousRealm = previousRealm;
    }

    public static void setRealmUnitState() {
        for (RealmUnit realmUnit : previousRealm.getDataUnits()) {
            log.info(realmUnit);
            setPreviousRealmUnit(realmUnit);
        }
    }

    public static RealmUnit getPreviousRealmUnit(String key) {
        return previousRealmUnit.get(key);
    }

    public static void setPreviousRealmUnit(RealmUnit previousRealm) {
        SheetState.previousRealmUnit.put(previousRealm.getCode(), previousRealm);
    }

    public static Map<String, XlsxImport> getState() {
        return state;
    }

    public static Set<String> getUpdateState() {
        return updateState;
    }

    public static void setUpdateState(Set<String> updateState) {
        SheetState.updateState = updateState;
    }

    public static void removeUpdateState(String key) {
        SheetState.updateState.remove(key);
    }

    public static RealmUnit getUpdatedRealms(String realmName) {
        return updateRows(realmName, SheetState::findUpdatedRows);
    }

    public static RealmUnit getDeletedRowsFromRealms(String realmName) {
        return updateRows(realmName, SheetState::findDeletedRows);
    }

    public static RealmUnit updateRows(String realmName, FIGetDeltaRows rowMethod) {
        Realm realm = getPreviousRealm();
        realm.init();
        return realm.getDataUnits().stream()
                .filter(d -> d.getCode().equals(realmName.toLowerCase()))
                .map(realmUnit -> {
                    RealmUnit previousRealm = SheetState.getPreviousRealmUnit(realmUnit.getCode());
                    realmUnit.setBaseEntitys(rowMethod.update(realmUnit.getBaseEntitys(), previousRealm.getBaseEntitys()));
                    realmUnit.setDataTypes(rowMethod.update(realmUnit.getDataTypes(), previousRealm.getDataTypes()));
                    realmUnit.setAttributes(rowMethod.update(realmUnit.getAttributes(), previousRealm.getAttributes()));
                    realmUnit.setEntityAttributes(rowMethod.update(realmUnit.getEntityAttributes(), previousRealm.getEntityAttributes()));
                    realmUnit.setValidations(rowMethod.update(realmUnit.getValidations(), previousRealm.getValidations()));
                    realmUnit.setQuestions(rowMethod.update(realmUnit.getQuestions(), previousRealm.getQuestions()));
                    realmUnit.setQuestionQuestions(rowMethod.update(realmUnit.getQuestionQuestions(), previousRealm.getQuestionQuestions()));
                    // realmUnit.setMessages(findUpdatedRows(realmUnit.messages, previousRealm.messages));
                    return realmUnit;
                }).findFirst().get();
    }
    
    public static Map<String, Map<String, String>> findDeletedRows(
            Map<String, Map<String, String>> newRows,
            Map<String, Map<String, String>> oldRows) {
        Optional<Map<String, Map<String, String>>> reduce = oldRows.entrySet().stream()
                .filter(o -> !newRows.containsKey(o.getKey())
                )
                .map(data -> {
                    Map<String, Map<String, String>> map = new HashMap<>();
                    map.put(data.getKey(), data.getValue());
                    return map;
                })
                .reduce((acc, n) -> {
                    acc.putAll(n);
                    return acc;
                });
        return reduce.orElseGet(HashMap::new);
    }

    public static Map<String, Map<String, String>> findUpdatedRows(
            Map<String, Map<String, String>> newRows,
            Map<String, Map<String, String>> oldRows) {
        Optional<Map<String, Map<String, String>>> reduce = newRows.entrySet().stream()
                .filter(o -> !oldRows.containsKey(o.getKey())
                        ||
                        !oldRows.containsValue(o.getValue())
                )
                .map(data -> {
                    Map<String, Map<String, String>> map = new HashMap<>();
                    map.put(data.getKey(), data.getValue());
                    return map;
                })
                .reduce((acc, n) -> {
                    acc.putAll(n);
                    return acc;
                });
        return reduce.orElseGet(HashMap::new);
    }

    private interface FIGetDeltaRows {
        public Map<String, Map<String, String>> update(Map<String, Map<String, String>> newRows, Map<String, Map<String, String>> oldRows);
    }
}
