package life.genny.bootq.models;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import life.genny.bootq.sheets.RealmUnit;
import life.genny.bootq.utils.GoogleSheetBuilder;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.validation.Validation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BatchLoading {

    private static boolean isSynchronise;

    private static final String ATT_PREFIX= "ATT_";
    private static final String SER_PREFIX= "SER_";
    private static final String DFT_PREFIX= "DFT_";
    private static final String DEP_PREFIX= "DEP_";
    private static final String UNQ_PREFIX= "UNQ_";

    private final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	AttributeUtils attributeUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	QuestionUtils questionUtils;

    public BatchLoading() {
    }

    public static boolean isSynchronise() {
        return isSynchronise;
    }

    public String constructKeycloakJson(final RealmUnit realm) {
        String clientId= realm.getCode();
        String masterRealm = "internmatch";
        String keycloakUrl = null;
        String keycloakSecret = null;
        String keycloakJson = null;

        keycloakUrl = realm.getKeycloakUrl();
        keycloakSecret = realm.getClientSecret();
        if ("internmatch".equals(clientId)) {
        keycloakJson = "{\n" + "  \"realm\": \"" + masterRealm + "\",\n" + "  \"auth-server-url\": \"" + keycloakUrl
                + "/auth\",\n" + "  \"ssl-required\": \"external\",\n" + "  \"resource\": \"" + clientId + "\",\n"
                + "  \"credentials\": {\n" + "    \"secret\": \"" + keycloakSecret + "\" \n" + "  },\n"
                + "  \"policy-enforcer\": {}\n" + "}";

        } else {
                   keycloakJson = "{\n" + "  \"realm\": \"" + masterRealm + "\",\n" + "  \"auth-server-url\": \"" + keycloakUrl
                + "/auth\",\n" + "  \"ssl-required\": \"external\",\n" + "  \"resource\": \"" + clientId + "\",\n"
                + "     \"public-client\": true,\n"
                + "  \"confidential-port\": 0\n" + "}";
        }

        log.info(String.format("[%s] Loaded keycloak.json:%s ", clientId, keycloakJson));
        return keycloakJson;
    }

    public void persistProjectOptimization(RealmUnit rx) {

        Instant start = Instant.now();
        persistValidations(rx.getValidations(), rx.getCode());
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistDatatypes(rx.getDataTypes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistAttributes(rx.getAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistBaseEntitys(rx.getDef_baseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished def_baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistBaseEntitys(rx.getBaseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistDefBaseEntityAttributes(rx.getDef_entityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished def_baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistBaseEntityAttributes(rx.getEntityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistQuestions(rx.getQuestions(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        persistQuestionQuestions(rx.getQuestionQuestions(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished question_question, cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

    /**
	 * Persist the validations
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistValidations(Map<String, Map<String, String>> project, String realmName) {
        for (Map<String, String> row : project.values()) {
            Validation validation = GoogleSheetBuilder.buildValidation(row, realmName);
			attributeUtils.saveValidation(validation);
        }
        log.info("Handled " + project.entrySet().size() + " Validations");
    }

    /**
	 * Persist the datatypes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDatatypes(Map<String, Map<String, String>> project, String realmName) {
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			DataType dataType = GoogleSheetBuilder.buildDataType(entry.getValue(), realmName);
			attributeUtils.saveDataType(dataType);
		}
    }

    /**
	 * Persist the attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistAttributes(Map<String, Map<String, String>> project, String realmName) {
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Attribute attribute = GoogleSheetBuilder.buildAttribute(entry.getValue(), realmName);
			attributeUtils.saveAttribute(attribute);
        }
        log.info("Handled " + project.entrySet().size() + " Attributes");
    }

    /**
	 * Persist the baseentitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistBaseEntitys(Map<String, Map<String, String>> project, String realmName) {
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            BaseEntity baseEntity = GoogleSheetBuilder.buildBaseEntity(entry.getValue(), realmName);
			beUtils.updateBaseEntity(baseEntity);
        }
        log.info("Handled " + project.entrySet().size() + " BaseEntityAttributes");
    }

    /**
	 * Persist the def baseentitys
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistDefBaseEntityAttributes(Map<String, Map<String, String>> project, String realmName) {

		DataType dttBoolean = attributeUtils.getDataType("DTT_BOOLEAN", false);
		DataType dttText = attributeUtils.getDataType("DTT_TEXT", false);

		Map<String, DataType> dttPrefixMap = Map.of(
			ATT_PREFIX, dttBoolean,
			SER_PREFIX, dttText,
			DFT_PREFIX, dttText,
			DEP_PREFIX, dttText,
			UNQ_PREFIX, dttText
		);

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Map<String, String> row = entry.getValue();
			String attributeCode = row.get("attributecode").replaceAll("^\"|\"$", "");
			if (Attribute.LNK_INCLUDE.equals(attributeCode)) {
				String[] codes = CommonUtils.cleanUpAttributeValue(row.get("valueString")).split(",");
				for (String code : codes) {
				// handle
				}
				continue;
			}
			// find or create attribute
			Attribute defAttr = attributeUtils.getAttribute(attributeCode);
			if (defAttr == null) {
				DataType dataType = dttPrefixMap.get(attributeCode.substring(0, 4));
				defAttr = new Attribute(attributeCode, attributeCode, dataType);
				defAttr.setRealm(realmName);
				attributeUtils.saveAttribute(defAttr);
			}
			EntityAttribute entityAttribute = GoogleSheetBuilder.buildEntityAttribute(row, realmName, defAttr.getCode());
			beaUtils.updateEntityAttribute(entityAttribute);
        }
    }

    /**
	 * Persist the baseentity attributes
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistBaseEntityAttributes(Map<String, Map<String, String>> project, String realmName) {
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            EntityAttribute entityAttribute = GoogleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName);
			beaUtils.updateEntityAttribute(entityAttribute);
        }
        log.info("Handled " + project.entrySet().size() + " BaseEntityAttributes");
    }

    /**
	 * Persist the questions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestions(Map<String, Map<String, String>> project, String realmName) {

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            Question question = GoogleSheetBuilder.buildQuestion(entry.getValue(), realmName);
			questionUtils.saveQuestion(question);
		}
        log.info("Handled " + project.entrySet().size() + " Questions");
    }

    /**
	 * Persist the questionQuestions
	 *
     * @param project The project sheets data
     * @param realmName The realm
     */
    public void persistQuestionQuestions(Map<String, Map<String, String>> project, String realmName) {

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            QuestionQuestion questionQuestion = GoogleSheetBuilder.buildQuestionQuestion(entry.getValue(), realmName);
			questionUtils.saveQuestionQuestion(questionQuestion);
        }
        log.info("Handled " + project.entrySet().size() + " QuestionQuestions");
    }

}
