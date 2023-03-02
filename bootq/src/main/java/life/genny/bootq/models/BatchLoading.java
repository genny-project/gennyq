package life.genny.bootq.models;


import life.genny.bootq.sheets.RealmUnit;
import life.genny.bootq.utils.GoogleSheetBuilder;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.validation.Validation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BatchLoading {

    private static boolean isSynchronise;
    
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

    public BatchLoading() {
    }

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
        persistDefBaseEntitys(rx.getDef_baseEntitys(), rx.getCode());
        persistBaseEntitys(rx.getBaseEntitys(), rx.getCode());
        persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode());
        persistBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
        persistQuestions(rx.getQuestions(), rx.getCode());
        persistQuestionQuestions(rx.getQuestionQuestions(), rx.getCode());
    }

    /**
	 * Persist a specific table.
	 *
     * @param rx
     * @param table
     */
    public void persistTable(RealmUnit rx, String table) {
		switch (table) {
			case "validation":
				persistValidations(rx.getValidations(), rx.getCode());
				break;
			case "datatype":
				persistDatatypes(rx.getDataTypes(), rx.getCode());
				break;
			case "attribute":
				persistAttributes(rx.getAttributes(), rx.getCode());
				break;
			case "def_baseentity":
				persistDefBaseEntitys(rx.getDef_baseEntitys(), rx.getCode());
				break;
			case "baseentity":
				persistBaseEntitys(rx.getBaseEntitys(), rx.getCode());
				break;
			case "def_entityattribute":
				persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode());
				break;
			case "entityattribute":
				persistBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
				break;
			case "question":
				persistQuestions(rx.getQuestions(), rx.getCode());
				break;
			case "questionquestion":
				persistQuestionQuestions(rx.getQuestionQuestions(), rx.getCode());
				break;
		}
    }

    /**
	 * Persist the validations
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistValidations(Map<String, Map<String, String>> project, String realmName) {

        Instant start = Instant.now();
        for (Map<String, String> row : project.values()) {
			Validation validation = googleSheetBuilder.buildValidation(row, realmName);
			attributeUtils.saveValidation(validation);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the datatypes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDatatypes(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				DataType dataType = googleSheetBuilder.buildDataType(entry.getValue(), realmName);
				attributeUtils.saveDataType(dataType);
			} catch (ItemNotFoundException e) {
				log.warn(e.getMessage());
			}
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished datatypes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistAttributes(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Attribute attribute = googleSheetBuilder.buildAttribute(entry.getValue(), realmName);
            attributeUtils.saveAttribute(attribute);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the def baseentitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDefBaseEntitys(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
		persistEntitys(project, realmName);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished definition baseentitys, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
	}

    /**
	 * Persist the baseentitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistBaseEntitys(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
		persistEntitys(project, realmName);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished baseentitys, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
	}

    /**
	 * Persist the entitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistEntitys(Map<String, Map<String, String>> project, String realmName) {
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				BaseEntity baseEntity = googleSheetBuilder.buildBaseEntity(entry.getValue(), realmName);
				beUtils.updateBaseEntity(baseEntity, false);
			} catch (ItemNotFoundException e) {
				log.warn(e.getMessage());
			}
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

		Map<String, DataType> dttPrefixMap = Map.of(
			Prefix.ATT_, dttBoolean,
			Prefix.SER_, dttText,
			Prefix.DFT_, dttText,
			Prefix.DEP_, dttText,
			Prefix.UNQ_, dttText
		);

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Map<String, String> row = entry.getValue();
			String attributeCode = row.get("attributecode").replaceAll("^\"|\"$", "");
			try {
				// find or create attribute
				Attribute defAttr = attributeUtils.getAttribute(attributeCode);
				if (defAttr == null) {
					DataType dataType = dttPrefixMap.get(attributeCode.substring(0, 4));
					defAttr = new Attribute(attributeCode, attributeCode, dataType);
					defAttr.setRealm(realmName);
					attributeUtils.saveAttribute(defAttr);
				}
				EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(row, realmName, defAttr.getCode());
				beaUtils.updateEntityAttribute(entityAttribute);
			} catch (ItemNotFoundException e) {
				log.warn(e.getMessage());
			}
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished definition entity attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the baseentity attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistBaseEntityAttributes(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName);
				beaUtils.updateEntityAttribute(entityAttribute);
			} catch (ItemNotFoundException e) { // ensure to print beCode and eaCode
				log.warn(new StringBuilder("Error occurred when building ")
                    .append(entry.getValue().get("baseentitycode")).append(":").append(entry.getValue().get("attributecode"))
                    .append(" - ").append(e.getMessage()).toString());
			}
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished entity attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the questions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestions(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				Question question = googleSheetBuilder.buildQuestion(entry.getValue(), realmName);
				questionUtils.saveQuestion(question);
			} catch (ItemNotFoundException e) {
				log.warn(e.getMessage());
			}
		}
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished questions, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

    /**
	 * Persist the questionQuestions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestionQuestions(Map<String, Map<String, String>> project, String realmName) {
        Instant start = Instant.now();

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				QuestionQuestion questionQuestion = googleSheetBuilder.buildQuestionQuestion(entry.getValue(), realmName);
				questionUtils.saveQuestionQuestion(questionQuestion);
			} catch (ItemNotFoundException e) {
				log.warn(e.getMessage());
			}
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished question questions, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

}
