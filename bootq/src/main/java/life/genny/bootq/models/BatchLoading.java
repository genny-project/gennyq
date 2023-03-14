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
import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.exception.runtime.BadDataException;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.ANSIColour;
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
import java.util.Collection;
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

        persistBaseEntities(rx.getDef_baseEntitys(), rx.getCode());
        persistBaseEntities(rx.getBaseEntitys(), rx.getCode());

        persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode());
        persistBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
        linkEntityAttributes(rx.getDef_entityAttributes().values(), rx.getCode());

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
                log.info("Finished baseentityies, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + rx.getBaseEntitys().size());
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
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			DataType dataType;
            try {
				dataType = googleSheetBuilder.buildDataType(entry.getValue(), realmName);
            } catch(BadDataException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }

            attributeUtils.saveDataType(dataType);
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Saved %s datatypes. Continuing...", count);
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
        long id = cm.getMaxAttributeId() + 1;
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Attribute attribute;
            try {
                attribute = googleSheetBuilder.buildAttribute(entry.getValue(), realmName);
            } catch(BadDataException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }

            if(attribute.getId() == null) {
                attribute.setId(id++);
            }
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Saved %s attributes. Continuing...", count);
            attributeUtils.saveAttribute(attribute);
            
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
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
        int count = 1;
        long id = cm.getMaxAttributeId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				BaseEntity baseEntity = googleSheetBuilder.buildBaseEntity(entry.getValue(), realmName);
                if (baseEntity.getId() == null) {
                    baseEntity.setId(id++);
                }
				beUtils.updateBaseEntity(baseEntity, false);
                if (count++ % LOG_BATCH_SIZE == 0)
                    log.debugf("Saved %s baseEntitys. Continuing...", count);
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
			Prefix.SER_, dttText, // TODO: Didn't the dropdowns start getting cached in SearchCaching for each product?
			Prefix.DFT_, dttText,
			Prefix.DEP_, dttText,
			Prefix.UNQ_, dttText
		);

        int count = 1;
        long attrId = cm.getMaxAttributeId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Map<String, String> row = entry.getValue();
            
            // ensure valid attribute, base entity both exist
            // For a DEF EA a valid attribute is one that exists in the attribute sheet
            // e.g ATT_PRI_NATIONALITY would have a valid attribute PRI_NATIONALITY, and ATT_PRI_NATIONALITY would still need to be autogenerated
            BaseEntity defBe;
            try {
                Map<Class<?>, Object> dependencies = validator.validateEntityAttribute(row, realmName);
                defBe = (BaseEntity) dependencies.get(BaseEntity.class);
            } catch(BadDataException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }

            // find or create attribute 
            Attribute defAttr;
            String attributeCode = row.get("attributecode");
            try {
                defAttr = attributeUtils.getAttribute(realmName, attributeCode);
            } catch (ItemNotFoundException e) {
                String combined = new StringBuilder(row.get("baseentitycode")).append(":").append(attributeCode).toString();
                log.trace(new StringBuilder("[DEF_EntityAttribute] Missing attribute ")
                    .append(attributeCode).append(" when building ").append(combined).append("! Creating!").toString());

                DataType dataType = dttPrefixMap.get(attributeCode.substring(0, 4));               
                defAttr = new Attribute(attributeCode, attributeCode, dataType);
                defAttr.setRealm(realmName);
                defAttr.setId(attrId++);
                attributeUtils.saveAttribute(defAttr);
                log.trace("Saving attribute: " + defAttr + " successful");
            }

            EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(row, realmName, defBe, defAttr);

            beaUtils.updateEntityAttribute(entityAttribute);
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s definition entity attributes. Continuing...", count);
        }

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished definition entity attributes, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
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
                defBe = beUtils.getBaseEntity(row.get("baseentitycode"));
            } catch(ItemNotFoundException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }

            Map<String, EntityAttribute> inheritedEas = beaUtils.getAllEntityAttributesInParent(Definition.from(defBe));

            for(Map.Entry<String, EntityAttribute> eas : inheritedEas.entrySet()) {
                EntityAttribute entityAttribute = eas.getValue().clone();
                entityAttribute.setBaseEntity(defBe);
                EntityAttribute newEa = defBe.addEntityAttribute(entityAttribute.getAttribute(), entityAttribute.getWeight(), entityAttribute.getInferred(), entityAttribute.getValue());
                newEa.setPrivacyFlag(entityAttribute.getPrivacyFlag());
                newEa.setConfirmationFlag(entityAttribute.getConfirmationFlag());

                beaUtils.updateEntityAttribute(newEa);
            }
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
        Instant start = Instant.now();
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            
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
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }

            EntityAttribute entityAttribute = googleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName, baseEntity, attribute);
            beaUtils.updateEntityAttribute(entityAttribute);

            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s entity attributes. Continuing...", count);
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
        int count = 1;
        long id = cm.getMaxQuestionId() + 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			Question question;
            try {
				question = googleSheetBuilder.buildQuestion(entry.getValue(), realmName);
			} catch(BadDataException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }
            question.setId(id++);
            questionUtils.saveQuestion(question);
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s questions. Continuing...", count);
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
        int count = 1;
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			try {
				QuestionQuestion questionQuestion = googleSheetBuilder.buildQuestionQuestion(entry.getValue(), realmName);
				questionUtils.saveQuestionQuestion(questionQuestion);
			} catch(BadDataException e) {
                log.error(ANSIColour.RED + e.getMessage() + ". Skipping" + ANSIColour.RESET);
                continue;
            }
            
            if (count++ % LOG_BATCH_SIZE == 0)
                log.debugf("Processed %s questionquestions. Continuing...", count);
        }
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished question questions, cost:" + timeElapsed.toMillis() + " millSeconds, items: " + project.entrySet().size());
    }

}
