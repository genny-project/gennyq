package life.genny.bootq.bootxport.bootx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public static RealmUnit getDeletedRowsFromRealms(String realmName) {
        Realm realm = getPreviousRealm();
        realm.init();
        RealmUnit updatedRealm = realm.getDataUnits().stream()
                .filter(d -> d.getCode().equals(realmName.toLowerCase()))
                .map(realmUnit -> {
                    RealmUnit previousRealm = SheetState.getPreviousRealmUnit(realmUnit.getCode());
                    realmUnit.setBaseEntitys(findDeletedRows(realmUnit.baseEntitys, previousRealm.baseEntitys));
                    realmUnit.setAsks(findDeletedRows(realmUnit.asks, previousRealm.asks));
                    realmUnit.setAttributeLinks(findDeletedRows(realmUnit.attributeLinks, previousRealm.attributeLinks));
                    realmUnit.setDataTypes(findDeletedRows(realmUnit.dataTypes, previousRealm.dataTypes));
                    realmUnit.setAttributes(findDeletedRows(realmUnit.attributes, previousRealm.attributes));
                    realmUnit.setEntityAttributes(findDeletedRows(realmUnit.entityAttributes, previousRealm.entityAttributes));
                    realmUnit.setValidations(findDeletedRows(realmUnit.validations, previousRealm.validations));
                    realmUnit.setQuestions(findDeletedRows(realmUnit.questions, previousRealm.questions));
                    realmUnit.setQuestionQuestions(findDeletedRows(realmUnit.questionQuestions, previousRealm.questionQuestions));
                    realmUnit.setNotifications(findDeletedRows(realmUnit.notifications, previousRealm.notifications));
                    realmUnit.setMessages(findDeletedRows(realmUnit.messages, previousRealm.messages));
                    return realmUnit;
                }).findFirst().get();
        return updatedRealm;
    }

    public static RealmUnit getUpdatedRealms(String realmName) {
        Realm realm = getPreviousRealm();
        realm.init();
        RealmUnit updatedRealm = realm.getDataUnits().stream()
                .filter(d -> d.getCode().equals(realmName.toLowerCase()))
                .map(realmUnit -> {
                    RealmUnit previousRealm = SheetState.getPreviousRealmUnit(realmUnit.getCode());
                    realmUnit.setBaseEntitys(findUpdatedRows(realmUnit.baseEntitys, previousRealm.baseEntitys));
                    realmUnit.setAsks(findUpdatedRows(realmUnit.asks, previousRealm.asks));
                    realmUnit.setAttributeLinks(findUpdatedRows(realmUnit.attributeLinks, previousRealm.attributeLinks));
                    realmUnit.setDataTypes(findUpdatedRows(realmUnit.dataTypes, previousRealm.dataTypes));
                    realmUnit.setAttributes(findUpdatedRows(realmUnit.attributes, previousRealm.attributes));
                    realmUnit.setEntityAttributes(findUpdatedRows(realmUnit.entityAttributes, previousRealm.entityAttributes));
                    realmUnit.setValidations(findUpdatedRows(realmUnit.validations, previousRealm.validations));
                    realmUnit.setQuestions(findUpdatedRows(realmUnit.questions, previousRealm.questions));
                    realmUnit.setQuestionQuestions(findUpdatedRows(realmUnit.questionQuestions, previousRealm.questionQuestions));
                    realmUnit.setNotifications(findUpdatedRows(realmUnit.notifications, previousRealm.notifications));
                    realmUnit.setMessages(findUpdatedRows(realmUnit.messages, previousRealm.messages));
                    return realmUnit;
                }).findFirst().get();
        return updatedRealm;
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
}
