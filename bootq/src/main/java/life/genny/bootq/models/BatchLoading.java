package life.genny.bootq.models;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import life.genny.bootq.utils.GoogleSheetBuilder;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.datatype.DataType;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CommonUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;
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
import java.util.*;

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

    private String decodePassword(String realm, String securityKey, String servicePass) {
        // TODO: Fix the hardcoding below:
        String initVector = "PRJ_INTERNMATCH"; // "PRJ_" + realm.toUpperCase();
        initVector = StringUtils.rightPad(initVector, 16, '*');
        String decrypt = decrypt(securityKey, initVector, servicePass);
        return decrypt;
    }

	public static String decrypt(String key, String initVector, String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

			return new String(original);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

    public void persistProjectOptimization(RealmUnit rx) {
        // service.setRealm(rx.getCode());

        String decrypt = decodePassword(rx.getCode(), rx.getSecurityKey(), rx.getServicePassword());

        Instant start = Instant.now();
        validationsOptimization(rx.getValidations(), rx.getCode());
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        log.info("Finished validations, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        datatypeOptimization(rx.getDataTypes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        attributesOptimization(rx.getAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        baseEntitysOptimization(rx.getDef_baseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished def_baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        baseEntitysOptimization(rx.getBaseEntitys(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished baseentity, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        def_baseEntityAttributesOptimization(rx.getDef_entityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished def_baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        baseEntityAttributesOptimization(rx.getEntityAttributes(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished baseentity_attribute, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        questionsOptimization(rx.getQuestions(), rx.getCode(), isSynchronise);
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished question, cost:" + timeElapsed.toMillis() + " millSeconds.");

        start = Instant.now();
        questionQuestionsOptimization(rx.getQuestionQuestions(), rx.getCode());
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log.info("Finished question_question, cost:" + timeElapsed.toMillis() + " millSeconds.");
    }

    public void validationsOptimization(Map<String, Map<String, String>> project, String realmName) {
        for (Map<String, String> row : project.values()) {
            Validation validation = GoogleSheetBuilder.buildValidation(row, realmName);
			beaUtils.saveValidation(validation);
        }
        log.info("Handled " + project.entrySet().size() + " Validations");
    }

    public Map<String, DataType> datatypeOptimization(Map<String, Map<String, String>> project, String realmName) {
        final Map<String, DataType> dataTypeMap = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
			DataType dataType = GoogleSheetBuilder.buildDataType(entry.getValue(), realmName);
			beaUtils.saveDataType(dataType);
		}
    }

    public void attributesOptimization(Map<String, Map<String, String>> project, String realmName) {

        for (Map.Entry<String, Map<String, String>> data : project.entrySet()) {
            Attribute attribute = GoogleSheetBuilder.buildAttribute(entry.getValue(), realmName);
			attributeUtils.saveAttribute(attribute);
        }
        log.info("Handled " + project.entrySet().size() + " Attributes");
    }

    public void baseEntitysOptimization(Map<String, Map<String, String>> project, String realmName) {

        HashMap<String, CodedEntity> codeBaseEntityMapping = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            BaseEntity baseEntity = GoogleSheetBuilder.buildBaseEntity(entry.getValue(), realmName);
			beUtils.updateBaseEntity(baseEntity);
        }
        log.info("Handled " + project.entrySet().size() + " BaseEntityAttributes");
    }

    public void def_baseEntityAttributesOptimization(Map<String, Map<String, String>> project, String realmName) {

		DataType dttBoolean = attributeUtils.getDataType("DTT_BOOLEAN", false);
		DataType dttText = attributeUtils.getDataType("DTT_TEXT", false);

		Map<String, DataType> defPrefixDataTypeMapping = Map.of(
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
				dataType = dttMap.get(attributeCode.substring(0, 4));
				defAttr = new Attribute(attributeCode, attributeCode, dataType);
				defAttr.setRealm(realmName);
				beaUtils.saveAttribute(defAttr);
			}
			EntityAttribute entityAttribute = GoogleSheetBuilder.buildEntityAttribute(row, realmName, defAttr);
			beaUtils.updateEntityAttribute(entityAttribute);
        }
    }

    public void baseEntityAttributesOptimization(Map<String, Map<String, String>> project, String realmName) {

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            EntityAttribute entityAttribute = GoogleSheetBuilder.buildEntityAttribute(entry.getValue(), realmName);
			beaUtils.updateEntityAttribute(entityAttribute);
        }
        log.info("Handled " + project.entrySet().size() + " BaseEntityAttributes");
    }

    public void questionQuestionsOptimization(Map<String, Map<String, String>> project, String realmName) {
        String tableName = "Question";
        List<Question> questionFromDB = service.queryTableByRealm(tableName, realmName);
        HashSet<String> questionCodeSet = new HashSet<>();
        HashMap<String, Question> questionHashMap = new HashMap<>();

        for (Question question : questionFromDB) {
            questionCodeSet.add(question.getCode());
            questionHashMap.put(question.getCode(), question);
        }

        tableName = "QuestionQuestion";
        List<QuestionQuestion> questionQuestionFromDB = service.queryTableByRealm(tableName, realmName);

        HashMap<String, QuestionQuestion> codeQuestionMapping = new HashMap<>();

        for (QuestionQuestion qq : questionQuestionFromDB) {
            String sourceCode = qq.getSourceCode();
            String targetCode = qq.getTarketCode();
            String uniqCode = sourceCode + "-" + targetCode;
            codeQuestionMapping.put(uniqCode, qq);
        }

        ArrayList<QuestionQuestion> questionQuestionInsertList = new ArrayList<>();
        ArrayList<QuestionQuestion> questionQuestionUpdateList = new ArrayList<>();
        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> entry : project.entrySet()) {
            total += 1;
            Map<String, String> queQues = entry.getValue();

            QuestionQuestion qq = GoogleSheetBuilder.buildQuestionQuestion(queQues, realmName, questionHashMap);
            if (qq == null) {
                invalid++;
                continue;
            }

            String parentCode = queQues.get("parentCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            if (parentCode == null) {
                parentCode = queQues.get("sourceCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));
            }

            String targetCode = queQues.get("targetCode".toLowerCase().replaceAll("^\"|\"$|_|-", ""));

            String uniqueCode = parentCode + "-" + targetCode;
            if (codeQuestionMapping.containsKey(uniqueCode.toUpperCase())) {
                if (isChanged(qq, codeQuestionMapping.get(uniqueCode.toUpperCase()))) {
                    questionQuestionUpdateList.add(qq);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                // insert new item
                questionQuestionInsertList.add(qq);
                newItem++;
            }
        }
        service.bulkInsertQuestionQuestion(questionQuestionInsertList);
        service.bulkUpdateQuestionQuestion(questionQuestionUpdateList, codeQuestionMapping);
        printSummary("QuestionQuestion", total, invalid, skipped, updated, newItem);
        questionFromDB = null;
        questionHashMap = null;
        questionQuestionFromDB = null;
        codeQuestionMapping = null;
        questionQuestionInsertList = null;
        questionQuestionUpdateList = null;
    }

    public void questionsOptimization(Map<String, Map<String, String>> project, String realmName, boolean isSynchronise) {
        // Get all questions from database
        String tableName = "Question";
        String mainRealm = GennySettings.mainrealm;
        List<Question> questionsFromDBMainRealm = new ArrayList<>();
        HashMap<String, Question> codeQuestionMappingMainRealm = new HashMap<>();

        if (!realmName.equals(mainRealm)) {
            questionsFromDBMainRealm = service.queryTableByRealm(tableName, mainRealm);
            for (Question q : questionsFromDBMainRealm) {
                codeQuestionMappingMainRealm.put(q.getCode(), q);
            }
        }

        List<Question> questionsFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Question> codeQuestionMapping = new HashMap<>();

        for (Question q : questionsFromDB) {
            codeQuestionMapping.put(q.getCode(), q);
        }

        // Get all Attributes from database
        tableName = "Attribute";
        List<Attribute> attributesFromDB = service.queryTableByRealm(tableName, realmName);
        HashMap<String, Attribute> attributeHashMap = new HashMap<>();

        for (Attribute attribute : attributesFromDB) {
            attributeHashMap.put(attribute.getCode(), attribute);
        }

        int invalid = 0;
        int total = 0;
        int skipped = 0;
        int newItem = 0;
        int updated = 0;

        for (Map.Entry<String, Map<String, String>> rawData : project.entrySet()) {
            total += 1;
            if (rawData.getKey().isEmpty()) {
                skipped += 1;
                continue;
            }

            Map<String, String> questions = rawData.getValue();
            String code = questions.get("code");

            Question question = GoogleSheetBuilder.buildQuestion(questions, attributeHashMap, realmName);
            if (question == null) {
                invalid++;
                continue;
            }

            Question existing = codeQuestionMapping.get(code.toUpperCase());
            if (existing == null) {
                if (isSynchronise) {
                    Question val = codeQuestionMappingMainRealm.get(code.toUpperCase());
                    if (val != null) {
                        val.setRealm(realmName);
                        service.updateRealm(val);
                        updated++;
                        continue;
                    }
                }
                log.debug("Inserting Question :"+question);
                service.insert(question);
                newItem++;
            } else {
                String name = questions.get("name");
                String html = questions.get("html");
                String directions = questions.get("directions");
                String helper = questions.get("helper");
                String icon = question.getIcon();
                existing.setName(name);
                existing.setHtml(html);
                existing.setDirections(directions);
                existing.setHelper(helper);
                existing.setIcon(icon);
                existing.setAttributeCode(question.getAttributeCode());
                existing.setAttribute(question.getAttribute());

                String oneshotStr = questions.get("oneshot");
                String readonlyStr = questions.get(GoogleSheetBuilder.READONLY);
                String mandatoryStr = questions.get(GoogleSheetBuilder.MANDATORY);
                boolean oneshot = GoogleSheetBuilder.getBooleanFromString(oneshotStr);
                boolean readonly = GoogleSheetBuilder.getBooleanFromString(readonlyStr);
                boolean mandatory = GoogleSheetBuilder.getBooleanFromString(mandatoryStr);
                existing.setOneshot(oneshot);
                existing.setReadonly(readonly);
                existing.setMandatory(mandatory);
                service.upsert(existing, codeQuestionMapping);
                updated++;
            }
        }
        printSummary("Question", total, invalid, skipped, updated, newItem);
        questionsFromDBMainRealm = null;
        codeQuestionMappingMainRealm = null;
        questionsFromDB = null;
        codeQuestionMapping = null;
        attributesFromDB = null;
        attributeHashMap = null;
    }

}
