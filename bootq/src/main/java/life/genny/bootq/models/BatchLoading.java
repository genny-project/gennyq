package life.genny.bootq.models;


import life.genny.bootq.sheets.realm.RealmUnit;
import life.genny.bootq.utils.GoogleSheetBuilder;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.constants.Prefix;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
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

import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class BatchLoading {

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
            if(attribute.getId() == null) {
                Long maxId = cm.getMaxAttributeId();
                log.error("Detected null attribute id for: " + attribute.getCode() + ". Setting to: " + maxId);
                attribute.setId(maxId + 1);
            }
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
			
            String baseEntityCode = row.get("baseentitycode");
            String attributeCode = row.get("attributecode");

				     // find or create attribute 
            Attribute defAttr;
            try {
                defAttr = attributeUtils.getAttribute(attributeCode);
            } catch (ItemNotFoundException e) {
                String combined = new StringBuilder(baseEntityCode).append(":").append(attributeCode).toString();
                log.trace(new StringBuilder("Missing attribute ")
                    .append(attributeCode).append(" when building ").append(combined).append("! Creating!").toString());
                
                DataType dataType = dttPrefixMap.get(attributeCode.substring(0, 4));                    
                defAttr = new Attribute(attributeCode, attributeCode, dataType);
                defAttr.setRealm(realmName);
                attributeUtils.saveAttribute(defAttr);
                log.trace("Saving attribute: " + defAttr + " successful");
            }
            try {
                EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(row, realmName, defAttr.getCode());
                beaUtils.updateEntityAttribute(entityAttribute);
            } catch(BadDataException e) {
                log.error("Error occurred when persisting: " + combined + ". " + e.getMessage());
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
			
            String baseEntityCode = entry.getValue().get("baseentitycode");
            String attributeCode = entry.getValue().get("attributecode");

            String combined = new StringBuilder(baseEntityCode).append(":").append(attributeCode).toString();

            EntityAttribute entityAttribute;
            try {
                log.trace("Building " + combined + " entityAttribute");
                entityAttribute = googleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName);

            } catch (BadDataException e) {
                log.error(new StringBuilder("(SKIPPING) Error occurred when building EA ")
                    .append(combined)
                    .append(" - ").append(e.getMessage()).toString());
                continue;
            }
            
            boolean success = beaUtils.updateEntityAttribute(entityAttribute);
            if(!success) {
                log.error("Error occured when persisting EntityAttribute: " + combined);
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
			Question question;
            try {
				question = googleSheetBuilder.buildQuestion(entry.getValue(), realmName);
			} catch (ItemNotFoundException e) {
				log.warn("Error Building Question: " + e.getMessage());
                continue;
			}
            
            // Verify question attribute id. The attribute tied to question at this point will exist (from buildQuestion)
            Attribute attribute;
            try {
                attribute = attributeUtils.getAttribute(question.getRealm(), question.getAttributeCode());
            } catch(ItemNotFoundException e) {
                log.error("Could not find attribute for question: " + question.getCode());
                attribute = question.getAttribute();
            }
            
            Long realAttributeId = attribute.getId();            
            if(question.getAttributeId() != realAttributeId) {
                log.trace("Found attribute ID mismatch for Question: " + question.getCode() + " and attribute: " + attribute.getCode() + ".\n" + 
                "\t- Question's Current Attribute ID: " + question.getAttributeId() +
                "\n\t- Attribute's Actual ID: " + realAttributeId +
                "\nSetting Question AttrID to Attribute ID: " + realAttributeId);
                question.setAttributeId(realAttributeId);
            }

            questionUtils.saveQuestion(question);

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
