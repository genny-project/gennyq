package life.genny.bootq.imprt;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import life.genny.bootq.exception.BootQException;
import life.genny.bootq.models.ModuleUnit;
import life.genny.bootq.models.RealmUnit;
import life.genny.bootq.security.BootSecurity;

@ApplicationScoped
public class ImportService {

    private static final String RANGE = "!A1:Z";

    private static final String VALIDATION = "Validation";
    private static final String DATATYPE = "DataType";
    private static final String ATTRIBUTE = "Attribute";
    private static final String ATTRIBUTE_LINK = "AttributeLink";
    private static final String BASE_ENTITY = "BaseEntity";
    private static final String QUESTION_QUESTION = "QuestionQuestion";
    private static final String QUESTION = "Question";
    private static final String ASK = "Ask";
    private static final String NOTIFICATION = "Notifications";
    private static final String MESSAGE = "Messages";
    private static final String ENTITY_ATTRIBUTE= "EntityAttribute";
    private static final String ENTITY_ENTITY= "EntityEntity";
    private static final String DEF_BASE_ENTITY = "DEF_BaseEntity";
    private static final String DEF_ENTITY_ATTRIBUTE = "DEF_EntityAttribute";

	private static final String[] TITLES = {
        VALIDATION,
        DATATYPE,
        ATTRIBUTE,
        ATTRIBUTE_LINK,
        BASE_ENTITY,
        QUESTION_QUESTION,
        QUESTION,
        ASK,
        NOTIFICATION,
        MESSAGE,
        ENTITY_ATTRIBUTE,
        ENTITY_ENTITY,
        DEF_BASE_ENTITY,
        DEF_ENTITY_ATTRIBUTE
	};

    public static final String[] CODE = { "code" };
    public static final String[] CODE_BA = { "baseEntityCode", "attributeCode" };
    public static final String[] CODE_TARGET_PARENT_LINK = { "targetCode", "parentCode", "linkCode", "Code", "SourceCode", };
    public static final String[] CODE_TARGET_PARENT = { "targetCode", "parentCode", "sourceCode", "linkCode", }; 
	public static final String[] CODE_QUESTION_SOURCE_TARGET = { "question_code", "sourceCode", "targetCode" };

	private static final Map<String, String[]> KEYS = new HashMap<>();
	static {
        KEYS.put(VALIDATION, CODE);
        KEYS.put(DATATYPE, CODE);
        KEYS.put(ATTRIBUTE, CODE);
        KEYS.put(ATTRIBUTE_LINK, CODE);
        KEYS.put(BASE_ENTITY, CODE);
        KEYS.put(QUESTION_QUESTION,  CODE_TARGET_PARENT);
        KEYS.put(QUESTION, CODE);
        KEYS.put(ASK, CODE_QUESTION_SOURCE_TARGET);
        KEYS.put(NOTIFICATION, CODE);
        KEYS.put(MESSAGE, CODE);
        KEYS.put(ENTITY_ATTRIBUTE, CODE_BA);
        KEYS.put(ENTITY_ENTITY, CODE_TARGET_PARENT_LINK);
        KEYS.put(DEF_BASE_ENTITY, CODE);
        KEYS.put(DEF_ENTITY_ATTRIBUTE, CODE_BA);
    }

	public static final String PROJECTS = "Projects";

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    @Inject
    BatchLoading bl;

	@Inject
	XlsxImport xlsxImport;

	@Inject
	BootSecurity security;

    public ImportService() {
    }

	/**
	 * @param sheetId
	 */
	public void process(String sheetId) {
		// TODO: make this recursive
		// fetch realm units
        List<RealmUnit> realmUnits = getRealmUnit(sheetId);
		for (RealmUnit realmUnit : realmUnits) {
			if (realmUnit.getDisable() || realmUnit.getSkipGoogleDoc()) {
				continue;
			}
			bl.persistProject(realmUnit);
			log.info("Finished loading sheet:" + realmUnit.getSheetId() + ", realm:" + realmUnit.getName());
		}
	}

    /**
     * @param sheetURI
     * @return
     */
    public List<RealmUnit> getRealmUnit(String sheetURI) {
        List<RealmUnit> list = new ArrayList<>();
        for (Map<String, String> rawData : xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, PROJECTS)) {
            if (!rawData.isEmpty()) {
				// init realm unit with id
				String sheetId = rawData.get("sheetID");
				RealmUnit realmUnit = new RealmUnit(rawData);
				// skip if necessary
				if (realmUnit.getSkipGoogleDoc()) {
					log.info("Skipping google doc for realm " + realmUnit.getName());
				} else {
					// find modules and set product code
					List<ModuleUnit> modules = getModuleUnit(sheetId);
					modules.forEach(m -> m.setProductCode(realmUnit.getProductCode()));
					realmUnit.setModules(modules);
				}
				list.add(realmUnit);
            }
        }
        return list;
    }

    /**
     * @param sheetURI
     * @return
     */
    public List<ModuleUnit> getModuleUnit(String sheetURI) {
        String modules = "Modules";
        return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, modules)
                .stream()
                .filter(rawData -> !rawData.isEmpty())
                .map(data -> {
                    ModuleUnit moduleUnit = new ModuleUnit(data);
					ArrayList<Sheet> sheets = getSheets(moduleUnit.getSheetId());
					// get tiles
					Set<String> titles = new HashSet<>();
					for (Sheet sheet : sheets) {
						SheetProperties sheetProperties = (SheetProperties) sheet.get("properties");
						String title = sheetProperties.getTitle();
						if (Set.of(TITLES).contains(title))
							titles.add(title);
					}

					ArrayList<ValueRange> valueRanges = getValueRanges(moduleUnit.getSheetId(), titles);
					processValues(moduleUnit, valueRanges, KEYS);
                    return moduleUnit;
                })
                .collect(Collectors.toList());
    }

    /**
     * @param service
     * @param sheetId
     * @return
     */
    private ArrayList<Sheet> getSheets(String sheetId) {
        // True if grid data should be returned.
        // This parameter is ignored if a field mask was set in the request.
        boolean includeGridData = false;
        try {
            Sheets.Spreadsheets.Get request = security.sheetsService.spreadsheets().get(sheetId);
            request.setRanges(new ArrayList<>());
            request.setIncludeGridData(includeGridData);

            Spreadsheet response = request.execute();
            return (ArrayList<Sheet>) response.get("sheets");
        } catch (Exception e) {
			throw new BootQException("Could not find sheets", e);
        }
    }

    /**
     * @param service
     * @param sheetId
     * @param titles
     * @return
     */
    private ArrayList<ValueRange> getValueRanges(String sheetId, Set<String> titles) {
        // The ranges to retrieve from the spreadsheet.
        List<String> ranges = new ArrayList<>();
        ArrayList<ValueRange> valueRanges = new ArrayList<>();
        for (String title: titles) {
            ranges.add(title + RANGE);
        }
        // True if grid data should be returned.
        // This parameter is ignored if a field mask was set in the request.
        try {
            Sheets.Spreadsheets.Values.BatchGet request = security.sheetsService.spreadsheets().values().batchGet(sheetId);
            request.setRanges(ranges);
            BatchGetValuesResponse response = request.execute();
            return (ArrayList<ValueRange>) response.get("valueRanges");
        } catch (IOException e) {
			throw new BootQException("Could not find sheets", e);
        }
    }

    /**
     * @param title
     * @param values
     * @param keyColumnsMapping
     * @param sheetURI
     * @return
     */
    private Map<String, Map<String, String>> getData(String title,
    List<List<Object>> values, Map<String, String[]> keyColumnsMapping, String sheetURI) {
        Map<String, Map<String, String>> tmp =  new HashMap<>();
        try {
            tmp = xlsxImport.mappingKeyHeaderToHeaderValues(values, keyColumnsMapping.get(title));
        } catch (Exception ex) {
            logFetchExceptionForSheets(ex.getMessage(), title, sheetURI);
        }
        return tmp;
    }

    /**
     * @param valueRanges
     * @param sheetURI
     * @param keyColumnsMapping
     */
    private void processValues(ModuleUnit moduleUnit, ArrayList<ValueRange> valueRanges,
                                Map<String, String[]> keyColumnsMapping) {
    	if (valueRanges == null) {
    		return;
    	}
		String sheetId = moduleUnit.getSheetId();
        for (ValueRange valueRange : valueRanges) {
            String title = valueRange.getRange().split("!")[0];
			List<List<Object>> values = valueRange.getValues();
			log.info("processing " + title + ", value size:" + values.size());
			switch (title) {
				case VALIDATION:
					moduleUnit.setValidations(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case DATATYPE:
					moduleUnit.setDataTypes(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case ATTRIBUTE:
					moduleUnit.setAttributes(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case BASE_ENTITY:
					moduleUnit.setBaseEntitys(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case QUESTION_QUESTION:
					moduleUnit.setQuestionQuestions(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case QUESTION:
					moduleUnit.setQuestions(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case ENTITY_ATTRIBUTE:
					moduleUnit.setEntityAttributes(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case ENTITY_ENTITY:
					moduleUnit.setEntityEntitys(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case DEF_BASE_ENTITY:
					moduleUnit.setDef_baseEntitys(getData(title, values, keyColumnsMapping, sheetId));
					break;
				case DEF_ENTITY_ATTRIBUTE:
					moduleUnit.setDef_entityAttributes(getData(title, values, keyColumnsMapping, sheetId));
					break;
				default:
					break;
            }
        }
    }

    public Map<String, Map<String, String>> fetchBaseEntity(String sheetURI) {
        String baseEntity = "BaseEntity";
        String key = sheetURI + baseEntity;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, baseEntity, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), baseEntity, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAttribute(String sheetURI) {
        String attribute = "Attribute";
        String key = sheetURI + attribute;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, attribute, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), attribute, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAttributeLink(String sheetURI) {
        String attributeLink = "AttributeLink";
        String key = sheetURI + attributeLink;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, attributeLink, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), attributeLink, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchQuestionQuestion(String sheetURI) {
        String questionQuestion = "QuestionQuestion";
        String key = sheetURI + questionQuestion;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, questionQuestion, CODE_TARGET_PARENT);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), questionQuestion, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchValidation(String sheetURI) {
        String validation = "Validation";
        String key = sheetURI + validation;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, validation, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), validation, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchDataType(String sheetURI) {
        String dataType = "DataType";
        String key = sheetURI + dataType;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, dataType, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), dataType, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchQuestion(String sheetURI) {
        String question = "Question";
        String key = sheetURI + question;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, question, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), question, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchAsk(String sheetURI) {
        String ask = "Ask";
        String key = sheetURI + ask;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, ask, CODE_QUESTION_SOURCE_TARGET);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), ask, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchNotifications(String sheetURI) {
        String notifications = "Notifications";
        String key = sheetURI + notifications;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, notifications, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), notifications, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchMessages(String sheetURI) {
        String messages = "Messages";
        String key = sheetURI + messages;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, messages, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), messages, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchEntityAttribute(String sheetURI) {
        String entityAttribute = "EntityAttribute";
        String key = sheetURI + entityAttribute;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityAttribute, CODE_BA);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), entityAttribute, sheetURI);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, String>> fetchEntityEntity(String sheetURI) {
        String entityEntity = "EntityEntity";
        String key = sheetURI + entityEntity;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityEntity, CODE_TARGET_PARENT_LINK);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), entityEntity, sheetURI);
            return new HashMap<>();
        }
    }

    // Baseentity Definition
    public Map<String, Map<String, String>> fetchDefBaseEntity(String sheetURI) {
        String baseEntity = "DEF_BaseEntity";
        String key = sheetURI + baseEntity;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, baseEntity, CODE);
        } catch (Exception e1) {
            logFetchExceptionForSheets(e1.getMessage(), baseEntity, sheetURI);
            return new HashMap<>();
        }
    }

    // EntityAttribute Definition
    public Map<String, Map<String, String>> fetchDefEntityAttribute(String sheetURI) {
        String entityAttribute = "DEF_EntityAttribute";
        String key = sheetURI + entityAttribute;
        try {
            return xlsxImport.mappingRawToHeaderAndValuesFmt(sheetURI, entityAttribute, CODE_BA);
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
