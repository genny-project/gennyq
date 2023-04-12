package life.genny.bootq.models;

import life.genny.bootq.models.reporting.LoadReport;
import life.genny.bootq.models.sheets.EReportCategoryType;
import life.genny.bootq.sheets.module.ModuleUnit;
import life.genny.bootq.sheets.realm.RealmUnit;
import life.genny.bootq.utils.GoogleSheetBuilder;
import life.genny.bootq.utils.SqliteHelper;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.validation.Validation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BatchLoading {

    public static final int LOG_BATCH_SIZE = 100;
    private static boolean isSynchronise;

    @Inject
    CacheManager cm;

    @Inject
    Logger log;

	@Inject
	AttributeUtils attributeUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	QuestionUtils questionUtils;

	@Inject
	GoogleSheetBuilder googleSheetBuilder;

    @Inject
    Validator validator;

    @Inject
    LoadReport loadReport;

    @Inject
    SqliteHelper sqliteHelper;

    public BatchLoading() { /* no-arg constructor */ }

    public static boolean isSynchronise() {
        return isSynchronise;
    }

    /**
	 * Persist the whole project.
	 *
     * @param rx
     */
    public void persistProject(RealmUnit rx) {
        persistValidations(rx.getValidations(), rx.getCode());
        persistDatatypes(rx.getDataTypes(), rx.getCode());
        persistAttributes(rx.getAttributes(), rx.getCode());

        persistBaseEntities(rx.getDef_baseEntitys(), rx.getCode());
        persistBaseEntities(rx.getBaseEntitys(), rx.getCode());

        persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode());
        persistBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
        linkEntityAttributes(rx.getDef_entityAttributes().values(), rx.getCode());

        persistQuestions(rx.getQuestions(), rx.getCode());
        persistQuestionQuestions(rx.getQuestionQuestions(), rx.getCode());
    }

    public void loadToSqlite(RealmUnit rx) throws SQLException, IOException {
        Set<String> moduleUnitNames = new HashSet<>();
        int i = 1;
        for (ModuleUnit moduleUnit : rx.getModule().getDataUnits()) {
            String moduleUnitName = moduleUnit.getModuleName();
            if(moduleUnitNames.contains(moduleUnitName)) {
                moduleUnitName = moduleUnitName + i++;
            }
            moduleUnitNames.add(moduleUnitName);
            log.infof("Loading into module: %s - %s", moduleUnit.getName(), moduleUnitName);
            Connection connection = null;
            try {
                connection = sqliteHelper.getConnectionToDatabase(moduleUnitName);
                loadToSqliteTable(connection, "validation", moduleUnit.getValidations());
                loadToSqliteTable(connection, "datatype", moduleUnit.getDataTypes());
                loadToSqliteTable(connection, "attribute", moduleUnit.getAttributes());
                loadToSqliteTable(connection, "def_baseentity", moduleUnit.getDef_baseEntitys());
                loadToSqliteTable(connection, "baseentity", moduleUnit.getBaseEntitys());
                loadToSqliteTable(connection, "def_entityattribute", moduleUnit.getDef_entityAttributes());
                loadToSqliteTable(connection, "entityattribute", moduleUnit.getEntityAttributes());
                loadToSqliteTable(connection, "question", moduleUnit.getQuestions());
                loadToSqliteTable(connection, "question_question", moduleUnit.getQuestionQuestions());
            } catch (SQLException | IOException e) {
                throw e;
            } finally {
                if (connection != null) {
                    sqliteHelper.closeConnection(connection);
                }
            }
            log.infof("Completed loading into module: %s", moduleUnitName);
        }
    }

    private void loadToSqliteTable(Connection connection, String tableName, Map<String, Map<String, String>> recordsMap) throws SQLException, IOException {
        sqliteHelper.createTable(connection, tableName, false);
        sqliteHelper.insertRecordIntoDatabase(connection, tableName, recordsMap.values());
    }

    public void loadDataInSqliteToDB(String realm, String sqliteDbName) throws SQLException {
        Connection connection = null;
        try {
            connection = sqliteHelper.getConnectionToDatabase(sqliteDbName);
            persistValidations(sqliteHelper.fetchRecordsFromTable(connection, "validation"), realm);
            persistDatatypes(sqliteHelper.fetchRecordsFromTable(connection, "datatype"), realm);
            persistAttributes(sqliteHelper.fetchRecordsFromTable(connection, "attribute"), realm);
            persistBaseEntities(sqliteHelper.fetchRecordsFromTable(connection, "def_baseentity"), realm);
            persistBaseEntities(sqliteHelper.fetchRecordsFromTable(connection, "baseentity"), realm);
            persistDefBaseEntityAttributes(sqliteHelper.fetchRecordsFromTable(connection, "def_entityattribute"), realm);
            persistBaseEntityAttributes(sqliteHelper.fetchRecordsFromTable(connection, "entityattribute"), realm);
            persistQuestions(sqliteHelper.fetchRecordsFromTable(connection, "question"), realm);
            persistQuestionQuestions(sqliteHelper.fetchRecordsFromTable(connection, "question_question"), realm);
            sqliteHelper.closeConnection(connection);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (connection != null) {
                sqliteHelper.closeConnection(connection);
            }
        }
    }

    public void loadSqlFileToSqlite(String dbName, File file) throws IOException, SQLException {
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        Connection connection = null;
        try {
            fileReader = new FileReader(file.getAbsolutePath());
            bufferedReader = new BufferedReader(fileReader);
            connection = sqliteHelper.getConnectionToDatabase(dbName);
            while (true) {
                String crudStatement = bufferedReader.readLine();
                if (crudStatement == null) {
                    break;
                }
                sqliteHelper.executeCrudStatement(connection, crudStatement);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (connection != null) {
                sqliteHelper.closeConnection(connection);
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    /**
	 * Persist a specific table.
	 *
     * @param rx
     * @param table
     */
    public void persistTable(RealmUnit rx, String table) {
		Duration timeElapsed;
        String realm = rx.getCode();
        switch (table) {
			case "validation":
				persistValidations(rx.getValidations(), realm);
				break;
			case "datatype":
				persistDatatypes(rx.getDataTypes(), realm);
				break;
			case "attribute":
				persistAttributes(rx.getAttributes(), realm);
				break;
			case "def_baseentity":
				timeElapsed = persistBaseEntities(rx.getDef_baseEntitys(), realm);
                log.info("Finished definition baseentities, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + rx.getDef_baseEntitys().size());
				break;
			case "baseentity":
                timeElapsed = persistBaseEntities(rx.getBaseEntitys(), realm);
                log.info("Finished baseentities, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + rx.getBaseEntitys().size());
				break;
			case "def_entityattribute":
				persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), realm);
                linkEntityAttributes(rx.getDef_entityAttributes().values(), realm);
				break;
			case "entityattribute":
				persistBaseEntityAttributes(rx.getEntityAttributes(), realm);
				break;
			case "question":
				persistQuestions(rx.getQuestions(), realm);
				break;
			case "questionquestion":
				persistQuestionQuestions(rx.getQuestionQuestions(), realm);
				break;
            case "linking":
                linkEntityAttributes(rx.getDef_entityAttributes().values(), realm);
                break;
            default:
                throw new IllegalStateException("Bad Table: " + table);
		}
    }

    /**
	 * Persist the validations
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistValidations(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        for (Map<String, String> row : project.values()) {
            
            Validation validation;
            try {
                validation = googleSheetBuilder.buildValidation(row, realmName);
            } catch (Exception e) {
                String entityInfo = realmName + ":" + row.get("code");
                loadReport.addBuildError(EReportCategoryType.VALIDATION, entityInfo, e);
                continue;
            }

            try {

                attributeUtils.saveValidation(validation);
                successFullySaved++;
            } catch (Exception e) {
                String entityInfo = realmName + ":" + row.get("code");
                loadReport.addPersistError(EReportCategoryType.VALIDATION, entityInfo, e);

            }
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + successFullySaved);
        if(!loadReport.hasErrors(EReportCategoryType.VALIDATION)) {
            loadReport.addSuccess(EReportCategoryType.VALIDATION, successFullySaved);
        }
    }

    /**
	 * Persist the datatypes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDatatypes(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			DataType dataType;
            try {
				dataType = googleSheetBuilder.buildDataType(entry.getValue(), realmName);
            } catch(BadDataException e) {
                String entityInfo = ("[" + realmName + "]: " + entry.getValue().get("code"));
                loadReport.addBuildError(EReportCategoryType.DATA_TYPE, entityInfo, e);
                continue;
            }
            
			try {
                attributeUtils.saveDataType(dataType);
                if (count++ % LOG_BATCH_SIZE == 0)
                    log.debugf("Saved %s datatypes. Continuing...", count);
                successFullySaved++;
			} catch (Exception e) {
                String entityInfo = realmName + ":" + entry.getValue().get("code");
                loadReport.addPersistError(EReportCategoryType.DATA_TYPE, entityInfo, e);
			}
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished datatypes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        if(!loadReport.hasErrors(EReportCategoryType.DATA_TYPE)) {
            loadReport.addSuccess(EReportCategoryType.DATA_TYPE, successFullySaved);
        }
    }

    /**
	 * Persist the attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistAttributes(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        long id = cm.getMaxAttributeId() + 1;
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Attribute attribute;
            try {
                attribute = googleSheetBuilder.buildAttribute(entry.getValue(), realmName);
            } catch(BadDataException e) {
                String entityInfo = realmName + ":" + entry.getValue().get("code");
                loadReport.addBuildError(EReportCategoryType.ATTRIBUTE, entityInfo, e);
                continue;
            }


            if(attribute.getId() == null) {
                attribute.setId(id++);
            }
            try {
                if (count++ % LOG_BATCH_SIZE == 0)
                    log.debugf("Saved %s attributes. Continuing...", count);
                attributeUtils.saveAttribute(attribute);
                successFullySaved++;
            } catch (Exception e) {
                String entityInfo = realmName + ":" + entry.getValue().get("code");
                loadReport.addPersistError(EReportCategoryType.ATTRIBUTE, entityInfo, e);
            }
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        if(!loadReport.hasErrors(EReportCategoryType.ATTRIBUTE)) {
            loadReport.addSuccess(EReportCategoryType.ATTRIBUTE, successFullySaved);
        }
    }

    /**
	 * Persist the def baseentities
	 *
     * @param project The project sheets data
     * @param realmName The realm
     * 
     * @return {@link Duration} containing time taken
     */
    public Duration persistBaseEntities(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
		persistEntities(project, realmName);
        Instant end = Instant.now();
        return Duration.between(start, end);
	}

    /**
	 * Persist the entitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistEntities(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        int count = 1;
        long id = cm.getMaxBaseEntityId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {

            BaseEntity baseEntity;
            try {
                baseEntity = googleSheetBuilder.buildBaseEntity(entry.getValue(), realmName);
            } catch(Exception e) {
                String entityInfo = realmName + ":" + entry.getValue().get("code");
                loadReport.addBuildError(EReportCategoryType.BASE_ENTITY, entityInfo, e);
                continue;
            }

            if (baseEntity.getId() == null) {
                baseEntity.setId(id++);
            }

            try {
				beUtils.updateBaseEntity(baseEntity, false);
                if (count++ % LOG_BATCH_SIZE == 0)
                    log.debugf("Saved %s baseEntitys. Continuing...", count);
			} catch (Exception e) {
                String entityInfo = realmName + ":" + entry.getValue().get("code");
                loadReport.addPersistError(EReportCategoryType.BASE_ENTITY, entityInfo, e);
            }
        }
        
        if(!loadReport.hasErrors(EReportCategoryType.BASE_ENTITY)) {
            loadReport.addSuccess(EReportCategoryType.BASE_ENTITY, successFullySaved);
        }
    }

    /**
	 * Persist the def baseentitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDefBaseEntityAttributes(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();

		DataType dttBoolean = attributeUtils.getDataType(realmName, "DTT_BOOLEAN", false);
		DataType dttText = attributeUtils.getDataType(realmName, "DTT_TEXT", false);

        Set<String> blacklistedDEFAttrs = Set.of(Prefix.SER_);

		Map<String, DataType> dttPrefixMap = Map.of(
			Prefix.ATT_, dttBoolean,
			Prefix.SER_, dttText, // TODO: Didn't the dropdowns start getting cached in SearchCaching for each product?
			Prefix.DFT_, dttText,
			Prefix.DEP_, dttText,
			Prefix.UNQ_, dttText
		);

        int successFullySaved = 0;
        int count = 1;
        long attrId = cm.getMaxAttributeId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Map<String, String> row = entry.getValue();

            String baseEntityCode = row.get("baseentitycode");
            String attributeCode = row.get("attributecode");

            // if(blacklistedDEFAttrs.contains(attributeCode.substring(0, 4))) {
            //     String entityInfo = realmName + ":" + baseEntityCode + ":" + attributeCode;
            //     loadReport.addBuildError(EReportCategoryType.DEF_BASEENTITY_ATTRIBUTE, entityInfo, 
            //         new Exception("Detected blacklisted definition attribute: " + attributeCode + ". Skipping"));
            //     continue;
            // }
            
            // ensure valid attribute, base entity both exist
            // For a DEF EA a valid attribute is one that exists in the attribute sheet
            // e.g ATT_PRI_NATIONALITY would have a valid attribute PRI_NATIONALITY, and ATT_PRI_NATIONALITY would still need to be autogenerated
            BaseEntity defBe;
            try {
                Map<Class<?>, Object> dependencies = validator.validateEntityAttribute(row, realmName);
                defBe = (BaseEntity) dependencies.get(BaseEntity.class);
            } catch(BadDataException e) {
                String entityInfo = realmName + ":" + baseEntityCode + ":" + attributeCode;
                loadReport.addBuildError(EReportCategoryType.DEF_BASEENTITY_ATTRIBUTE, entityInfo, e);
                continue;
            }

            // find or create attribute 
            Attribute defAttr;
            try {
                defAttr = attributeUtils.getAttribute(realmName, attributeCode);
            } catch (ItemNotFoundException e) {
                DataType dataType = dttPrefixMap.get(attributeCode.substring(0, 4));
                defAttr = new Attribute(attributeCode, attributeCode, dataType);
                defAttr.setRealm(realmName);
                defAttr.setId(attrId++);
                try {
                    attributeUtils.saveAttribute(defAttr);
                    log.trace("Saving attribute: " + defAttr + " successful");
                } catch(Exception persistException) {
                    String entityInfo = realmName + ":" + defAttr.getCode();
                    loadReport.addPersistError(EReportCategoryType.ATTRIBUTE, entityInfo, persistException);
                    continue;
                }
            }

            if(StringUtils.isBlank(defAttr.getDttCode())) {
                log.warn("Detected blank dtt code at: " + baseEntityCode + ":" + attributeCode + ". Manually assigning based on prefix");
                String pref = attributeCode.substring(0, 4);
                DataType dataType = dttPrefixMap.get(pref);
                defAttr.setDataType(dataType);
                attributeUtils.saveAttribute(defAttr);
            }
            
            EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(row, realmName, defBe, defAttr);

            try {
                beaUtils.updateEntityAttribute(entityAttribute);
                successFullySaved++;
            } catch (Exception e) {
                String entityInfo = realmName + ":" + baseEntityCode + ":" + attributeCode;
                loadReport.addPersistError(EReportCategoryType.DEF_BASEENTITY_ATTRIBUTE, entityInfo, e);
            }

            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s definition entity attributes. Continuing...", count);
        }

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished definition entity attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        if(!loadReport.hasErrors(EReportCategoryType.DEF_BASEENTITY_ATTRIBUTE)) {
            loadReport.addSuccess(EReportCategoryType.DEF_BASEENTITY_ATTRIBUTE, successFullySaved);
        }
    }

    /**
	 * Link DEF BaseEntities
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void linkEntityAttributes(Collection<Map<String, String>> entityAttributeRows, String realmName) {
        Instant start = Instant.now();

        for (Map<String, String> row : entityAttributeRows) {
            String lnkAttributeCode = row.get("attributecode");

            // Find All LNK_INCLUDES
            if(!lnkAttributeCode.equals(Attribute.LNK_INCLUDE))
                continue;

            BaseEntity defBe;
            try {
                defBe = beUtils.getBaseEntity(realmName, row.get("baseentitycode"));
            } catch(ItemNotFoundException e) {
                String entityInfo = realmName + ":" + row.get("baseentitycode");
                loadReport.addBuildError(EReportCategoryType.LINKING_ENTITIES, entityInfo, e);
                continue;
            }

            log.debug("Found LNK_INCLUDE for BE: " + defBe);

            Map<String, EntityAttribute> inheritedEas = beaUtils.getAllEntityAttributesInParent(Definition.from(defBe));
            log.debug("Found " + inheritedEas.size() + " inherited entity attributes");

            for(Map.Entry<String, EntityAttribute> eas : inheritedEas.entrySet()) {
                EntityAttribute entityAttribute = eas.getValue();
                log.trace("Adding " + defBe.getCode() + ":" + entityAttribute.getAttributeCode() + ", value: " + entityAttribute.getValue());
                Attribute attribute;
                try {
                    attribute = attributeUtils.getAttribute(entityAttribute.getRealm(), entityAttribute.getAttributeCode(), true);
                } catch(ItemNotFoundException e) {
                    String entityInfo = realmName + ":" + defBe.getCode() + ":" + entityAttribute.getAttributeCode();
                    loadReport.addBuildError(EReportCategoryType.LINKING_ENTITIES, entityInfo, e);
                    continue;
                }
                    
                entityAttribute.setAttribute(attribute);
                EntityAttribute newEa = defBe.addEntityAttribute(attribute, entityAttribute.getWeight() != null ? entityAttribute.getWeight() : 0.0, entityAttribute.getInferred(), entityAttribute.getValue());

                newEa.setPrivacyFlag(entityAttribute.getPrivacyFlag());
                newEa.setConfirmationFlag(entityAttribute.getConfirmationFlag());
                newEa.setCapabilityRequirements(entityAttribute.getCapabilityRequirements());
                try {
                    beaUtils.updateEntityAttribute(newEa);
                } catch(Exception e) {
                    String entityInfo = realmName + ":" + newEa.getBaseEntityCode() + ":" + newEa.getAttributeCode();
                    loadReport.addPersistError(EReportCategoryType.LINKING_ENTITIES, entityInfo, e);
                }
            }
        }
        if(!loadReport.hasErrors(EReportCategoryType.LINKING_ENTITIES)) {
            loadReport.addSuccess(EReportCategoryType.LINKING_ENTITIES, null);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished linking cost:" + timeElapsed.toMillis() + " millSeconds");
    }

    /**
	 * Persist the baseentity attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistBaseEntityAttributes(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            String baseEntityCode = entry.getValue().get("baseentitycode");
            String attributeCode = entry.getValue().get("attributecode");
            
            BaseEntity baseEntity;
            Attribute attribute;
            
            try {
                log.trace(new StringBuilder("Building ")
                        .append(entry.getValue().get("baseentitycode")).append(":").append(entry.getValue().get("attributecode"))
                        .append(" entityAttribute")
                        .toString());
                Map<Class<?>, Object> dependencies = validator.validateEntityAttribute(entry.getValue(), realmName);
                baseEntity = (BaseEntity) dependencies.get(BaseEntity.class);
                attribute = (Attribute) dependencies.get(Attribute.class);
            } catch(BadDataException e) {
                String entityInfo = realmName + ":" + baseEntityCode + ":" + attributeCode;
                loadReport.addBuildError(EReportCategoryType.BASEENTITY_ATTRIBUTE, entityInfo, e);
                continue;
            }

            EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName, baseEntity, attribute);

            try {
                beaUtils.updateEntityAttribute(entityAttribute);
            } catch (Exception e) {
                String entityInfo = realmName + ":" + baseEntityCode + ":" + attributeCode;
                loadReport.addPersistError(EReportCategoryType.BASEENTITY_ATTRIBUTE, entityInfo, e);
                continue;
            }

            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s entity attributes. Continuing...", count);
        }

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished entity attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        if(!loadReport.hasErrors(EReportCategoryType.BASEENTITY_ATTRIBUTE)) {
            loadReport.addSuccess(EReportCategoryType.BASEENTITY_ATTRIBUTE, successFullySaved);
        }
    }

    /**
	 * Persist the questions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestions(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        int count = 1;
        long id = cm.getMaxQuestionId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			Question question;
            try {
				question = googleSheetBuilder.buildQuestion(entry.getValue(), realmName);
			} catch(BadDataException e) {
                String code = entry.getValue().get("code");
                String entityInfo = realmName + ":" + code;
                loadReport.addBuildError(EReportCategoryType.QUESTION, entityInfo, e);
                continue;
            }

            // only null id if hasn't been set in buildQuestion (preexisting question found)
            if(question.getId() == null)
                question.setId(id++);

            try {
                questionUtils.saveQuestion(question);
            } catch (Exception e) {
                String entityInfo = realmName + ":" + question.getCode();
                loadReport.addPersistError(EReportCategoryType.QUESTION, entityInfo, e);
            }

            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s questions. Continuing...", count);
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished questions, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        
        if(!loadReport.hasErrors(EReportCategoryType.QUESTION)) {
            loadReport.addSuccess(EReportCategoryType.QUESTION, successFullySaved);
        }
    }

    /**
	 * Persist the questionQuestions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestionQuestions(Map<String, Map<String, String>> project, String realmName) {
        int successFullySaved = 0;
        Instant start = Instant.now();
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            QuestionQuestion questionQuestion;
			try {
				questionQuestion = googleSheetBuilder.buildQuestionQuestion(entry.getValue(), realmName);
			} catch(BadDataException e) {
                Map<String, String> row = entry.getValue();
                String parentCode = row.get("parentcode");
                String targetCode = row.get("targetcode");
                String entityInfo = realmName + ":" + parentCode + ":" + targetCode;
                loadReport.addBuildError(EReportCategoryType.QUESTION_QUESTION, entityInfo, e);
                continue;
            }
            
            Map<String, String> row = entry.getValue();

            String parentCode = row.get("parentcode");
            String targetCode = row.get("targetcode");
            try {
				questionUtils.saveQuestionQuestion(questionQuestion);
			} catch (Exception e) {
                String entityInfo = realmName + ":" + parentCode + ":" + targetCode;
                loadReport.addPersistError(EReportCategoryType.QUESTION_QUESTION, entityInfo, e);
			}
            
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s questionquestions. Continuing...", count);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished question questions, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
        
        if(!loadReport.hasErrors(EReportCategoryType.QUESTION_QUESTION)) {
            loadReport.addSuccess(EReportCategoryType.QUESTION_QUESTION, successFullySaved);
        }
    }
}
