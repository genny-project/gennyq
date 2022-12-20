package life.genny.bootq.bootxport.bootx;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

public class ImportService {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private Map<String, XlsxImport> state;
    private BatchLoadMode mode;

    public XlsxImport createXlsImport(String key) {
        if (SheetState.getUpdateState().contains(key)) {
            XlsxImportOnline xlsxImportOnline = new XlsxImportOnline(GoogleImportService.getInstance().getService());
            state.put(key, xlsxImportOnline);
            SheetState.removeUpdateState(key);
            log.info("The state it is being updated... " + key);
            return xlsxImportOnline;
        }
        if (state.containsKey(key))
            return state.get(key);
        if (mode == BatchLoadMode.ONLINE) {
            log.info("Creating a new Import service for " + key);
            XlsxImportOnline xlsxImportOnline = new XlsxImportOnline(GoogleImportService.getInstance().getService());
            state.put(key, xlsxImportOnline);
            return xlsxImportOnline;
        } else {
            return new XlsxImportOffline(null);
        }
    }

    public ImportService(BatchLoadMode mode, Map<String, XlsxImport> state) {
        this.mode = mode;
        this.state = state;
    }

    public List<RealmUnit> fetchRealmUnit(String sheetURI) {
        String projects = "Projects";
        String key = sheetURI + projects;
        XlsxImport createXlsImport = createXlsImport(key);
        List<RealmUnit> list = new ArrayList<>();
        for (Map<String, String> rawData : createXlsImport
                .mappingRawToHeaderAndValuesFmt(sheetURI, projects)) {
            if (!rawData.isEmpty()) {
                RealmUnit name = new RealmUnit(mode, rawData);
                list.add(name);
            }
        }
        return list;
    }

    public List<ModuleUnit> fetchModuleUnit(String sheetURI) {
        String modules = "Modules";
        String key = sheetURI + modules;
        XlsxImport createXlsImport = createXlsImport(key);
        return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, modules)
                .stream()
                .filter(rawData -> !rawData.isEmpty())
                .map(d1 -> {
                    ModuleUnit moduleUnit = new ModuleUnit(mode, d1.get("sheetID".toLowerCase()));
                    moduleUnit.setName(d1.get("name"));
                    return moduleUnit;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, String>> fetchBaseEntity(String sheetURI) {
        String baseEntity = "BaseEntity";
        String key = sheetURI + baseEntity;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, baseEntity, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), baseEntity, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAttribute(String sheetURI) {
        String attribute = "Attribute";
        String key = sheetURI + attribute;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, attribute, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), attribute, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAttributeLink(String sheetURI) {
        String attributeLink = "AttributeLink";
        String key = sheetURI + attributeLink;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, attributeLink, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), attributeLink, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchQuestionQuestion(String sheetURI) {
        String questionQuestion = "QuestionQuestion";
        String key = sheetURI + questionQuestion;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, questionQuestion, DataKeyColumn.CODE_TARGET_PARENT);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), questionQuestion, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchValidation(String sheetURI) {
        String validation = "Validation";
        String key = sheetURI + validation;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, validation, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), validation, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchDataType(String sheetURI) {
        String dataType = "DataType";
        String key = sheetURI + dataType;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, dataType, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), dataType, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchQuestion(String sheetURI) {
        String question = "Question";
        String key = sheetURI + question;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, question, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), question, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAsk(String sheetURI) {
        String ask = "Ask";
        String key = sheetURI + ask;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, ask, DataKeyColumn.CODE_QUESTION_SOURCE_TARGET);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), ask, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchNotifications(String sheetURI) {
        String notifications = "Notifications";
        String key = sheetURI + notifications;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, notifications, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), notifications, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchMessages(String sheetURI) {
        String messages = "Messages";
        String key = sheetURI + messages;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, messages, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), messages, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchEntityAttribute(String sheetURI) {
        String entityAttribute = "EntityAttribute";
        String key = sheetURI + entityAttribute;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityAttribute, DataKeyColumn.CODE_BA);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), entityAttribute, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchEntityEntity(String sheetURI) {
        String entityEntity = "EntityEntity";
        String key = sheetURI + entityEntity;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityEntity, DataKeyColumn.CODE_TARGET_PARENT_LINK);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), entityEntity, sheetURI);
            return new HashMap<>();
        }
    }

    // Baseentity Definition
    public Map<String, Map<String, String>> fetchDefBaseEntity(String sheetURI) {
        String baseEntity = "DEF_BaseEntity";
        String key = sheetURI + baseEntity;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, baseEntity, DataKeyColumn.CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), baseEntity, sheetURI);
            return new HashMap<>();
        }
    }

    // EntityAttribute Definition
    public Map<String, Map<String, String>> fetchDefEntityAttribute(String sheetURI) {
        String entityAttribute = "DEF_EntityAttribute";
        String key = sheetURI + entityAttribute;
        XlsxImport createXlsImport = createXlsImport(key);
        try {
            return createXlsImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityAttribute, DataKeyColumn.CODE_BA);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), entityAttribute, sheetURI);
            return new HashMap<>();
        }
    }

    private void logFetchExceptionForSheets(String exception, String sheetName, String sheetURI) {
        log.error("ATTENTION!, Exception:"  +  exception + " occurred when fetching sheetName:" + sheetName
        + ", sheetURI:" + sheetURI + ", return EMPTY HashMap!!!");
    }
}
