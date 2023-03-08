package life.genny.qwandaq.utils;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.serialization.baseentity.BaseEntity;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.serialization.question.QuestionKey;
import life.genny.qwandaq.serialization.questionquestion.QuestionQuestionKey;
import org.apache.commons.lang3.StringUtils;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class QuestionUtils {

    public static final String ATTRIBUTE_CODE = "attributeCode";
    public static final String DIRECTIONS = "directions";
    public static final String HELPER = "helper";
    public static final String HTML = "html";
    public static final String ICON = "icon";
    public static final String MANDATORY = "mandatory";
    public static final String ONESHOT = "oneshot";
    public static final String PLACEHOLDER = "placeholder";
    public static final String READONLY = "readonly";
    public static final String SOURCE_CODE = "sourceCode";
    public static final String TARGET_CODE = "targetCode";
    public static final String DISABLED = "disabled";
    public static final String HIDDEN = "hidden";
    public static final String VERSION = "version";
    public static final String WEIGHT = "weight";
    public static final String PARENT_CODE = "parentCode";
    public static final String CHILD_CODE = "childCode";

    public static Map<BaseEntityKey, Question> questionsLocalCache = new HashMap<>();

    public static Map<BaseEntityKey, QuestionQuestion> questionQuestionsLocalCache = new HashMap<>();
    
    @Inject
    Logger log;

    @Inject
    CacheManager cacheManager;

    @Inject
    BaseEntityUtils beUtils;

    @Inject
    AttributeUtils attributeUtils;

    public life.genny.qwandaq.entity.BaseEntity getPersistableBaseEntityFromQuestion(Question question) {
        life.genny.qwandaq.entity.BaseEntity baseEntity = (life.genny.qwandaq.entity.BaseEntity) getSerializableBaseEntityFromQuestion(question).toPersistableCoreEntity();
        List<life.genny.qwandaq.attribute.EntityAttribute> persistableBaseEntityAttributes = new LinkedList<>();
        List<EntityAttribute> serializableBaseEntityAttributes = getSerializableBaseEntityAttributesFromQuestion(question);
        serializableBaseEntityAttributes.forEach(baseEntityAttribute -> persistableBaseEntityAttributes.add(baseEntityAttribute));
        baseEntity.setBaseEntityAttributes(persistableBaseEntityAttributes);
        return baseEntity;
    }

    public BaseEntity getSerializableBaseEntityFromQuestion(Question question) {
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.setCode(question.getCode());
        baseEntity.setCreated(question.getCreated());
        baseEntity.setName(question.getName());
        baseEntity.setRealm(question.getRealm());
        baseEntity.setStatus(question.getStatus().ordinal());
        baseEntity.setUpdated(question.getUpdated());
        return baseEntity;
    }

    public List<EntityAttribute> getSerializableBaseEntityAttributesFromQuestion(Question question) {
        List<EntityAttribute> attributes = new LinkedList<>();
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, ATTRIBUTE_CODE, question.getAttributeCode()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, DIRECTIONS, question.getDirections()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, HELPER, question.getHelper()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, HTML, question.getHtml()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, ICON, question.getIcon()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, MANDATORY, question.getMandatory()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, ONESHOT, question.getOneshot()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, PLACEHOLDER, question.getPlaceholder()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestion(question, READONLY, question.getReadonly()));
        return attributes;
    }

    public EntityAttribute createSerializableBaseEntityAttributeFromQuestion(Question question, String attributeCode, Object value) {
        EntityAttribute attribute = new EntityAttribute();
        attribute.setRealm(question.getRealm());
        attribute.setBaseEntityCode(question.getCode());
        attribute.setAttributeCode(attributeCode);
        attribute.setCreated(question.getCreated());
        attribute.setUpdated(question.getUpdated());
        if (value instanceof Integer) {
            attribute.setValueInteger((Integer) value);
        } else if (value instanceof LocalDateTime) {
            attribute.setValueDateTime((LocalDateTime) value);
        } else if (value instanceof LocalTime) {
            attribute.setValueTime((LocalTime) value);
        } else if (value instanceof Long) {
            attribute.setValueLong((Long) value);
        } else if (value instanceof Double) {
            attribute.setValueDouble((Double) value);
        } else if (value instanceof Boolean) {
            attribute.setValueBoolean((Boolean) value);
        } else if (value instanceof LocalDate) {
            attribute.setValueDate((LocalDate) value);
        } else if (value instanceof Money) {
            attribute.setValueMoney((Money) value);
        } else {
            attribute.setValueString(value.toString());
        }
        return attribute;
    }

    public life.genny.qwandaq.entity.BaseEntity getPersistableBaseEntityFromQuestionQuestion(QuestionQuestion questionQuestion) {
        life.genny.qwandaq.entity.BaseEntity baseEntity = (life.genny.qwandaq.entity.BaseEntity) getSerializableBaseEntityFromQuestionQuestion(questionQuestion).toPersistableCoreEntity();
        List<life.genny.qwandaq.attribute.EntityAttribute> persistableBaseEntityAttributes = new LinkedList<>();
        List<EntityAttribute> serializableBaseEntityAttributes = getSerializableBaseEntityAttributesFromQuestionQuestion(questionQuestion);
        serializableBaseEntityAttributes.forEach(baseEntityAttribute -> persistableBaseEntityAttributes.add(baseEntityAttribute));
        baseEntity.setBaseEntityAttributes(persistableBaseEntityAttributes);
        return baseEntity;
    }

    public BaseEntity getSerializableBaseEntityFromQuestionQuestion(QuestionQuestion questionQuestion) {
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.setCode(questionQuestion.getParentCode() + BaseEntityKey.BE_KEY_DELIMITER + questionQuestion.getChildCode());
        baseEntity.setCreated(questionQuestion.getCreated());
        baseEntity.setRealm(questionQuestion.getRealm());
        baseEntity.setUpdated(questionQuestion.getUpdated());
        return baseEntity;
    }

    public List<EntityAttribute> getSerializableBaseEntityAttributesFromQuestionQuestion(QuestionQuestion questionQuestion) {
        List<EntityAttribute> attributes = new LinkedList<>();
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, PARENT_CODE, questionQuestion.getParentCode()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, CHILD_CODE, questionQuestion.getChildCode()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, DISABLED, questionQuestion.getDisabled()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, HIDDEN, questionQuestion.getHidden()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, ICON, questionQuestion.getIcon()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, MANDATORY, questionQuestion.getMandatory()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, READONLY, questionQuestion.getReadonly()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, VERSION, questionQuestion.getVersion()));
        attributes.add(createSerializableBaseEntityAttributeFromQuestionQuestion(questionQuestion, WEIGHT, questionQuestion.getWeight()));
        return attributes;
    }

    public EntityAttribute createSerializableBaseEntityAttributeFromQuestionQuestion(QuestionQuestion questionQuestion, String attributeCode, Object value) {
        EntityAttribute attribute = new EntityAttribute();
        attribute.setRealm(questionQuestion.getRealm());
        attribute.setBaseEntityCode(questionQuestion.getParentCode() + BaseEntityKey.BE_KEY_DELIMITER + questionQuestion.getChildCode());
        attribute.setAttributeCode(attributeCode);
        attribute.setCreated(questionQuestion.getCreated());
        attribute.setUpdated(questionQuestion.getUpdated());
        if (value instanceof Integer) {
            attribute.setValueInteger((Integer) value);
        } else if (value instanceof LocalDateTime) {
            attribute.setValueDateTime((LocalDateTime) value);
        } else if (value instanceof LocalTime) {
            attribute.setValueTime((LocalTime) value);
        } else if (value instanceof Long) {
            attribute.setValueLong((Long) value);
        } else if (value instanceof Double) {
            attribute.setValueDouble((Double) value);
        } else if (value instanceof Boolean) {
            attribute.setValueBoolean((Boolean) value);
        } else if (value instanceof LocalDate) {
            attribute.setValueDate((LocalDate) value);
        } else if (value instanceof Money) {
            attribute.setValueMoney((Money) value);
        } else {
            attribute.setValueString(value.toString());
        }
        return attribute;
    }

    public Question getQuestionFromQuestionCode(String productCode, String questionCode) {
        return cacheManager.getQuestion(productCode, questionCode);
    }

    public Question getQuestionFromBaseEntityCode(String productCode, String baseEntityCode) {
        BaseEntityKey baseEntityKey = new BaseEntityKey(productCode, baseEntityCode);
        Question question = getQuestionInLocalCache(baseEntityKey);
        if (question != null) {
            return question;
        }
        life.genny.qwandaq.entity.BaseEntity questionBE = beUtils.getBaseEntity(productCode, baseEntityCode);
        log.info("Question Code From BaseEntity = " + baseEntityCode);
        question = createQuestionFromBaseEntity(questionBE);
        questionsLocalCache.put(baseEntityKey, question);
        return question;
    }

    private Question getQuestionInLocalCache(BaseEntityKey baseEntityKey) {
        Question question = questionsLocalCache.get(baseEntityKey);
        if (question != null) {// && !question.getUpdated().isBefore(baseEntity.getUpdated())) {
            return question;
        }
        return null;
    }

    private Question createQuestionFromBaseEntity(life.genny.qwandaq.entity.BaseEntity baseEntity) {
        Question question = new Question();
        String productCode = baseEntity.getRealm();
        String baseEntityCode = baseEntity.getCode();
        life.genny.qwandaq.entity.BaseEntity questionBE = beUtils.getBaseEntity(baseEntity.getRealm(), baseEntityCode);
        log.info("Question Code From BaseEntity = " + baseEntityCode);
        question.setCode(baseEntityCode);
        question.setCreated(questionBE.getCreated());
        question.setName(questionBE.getName());
        question.setRealm(productCode);
        question.setStatus(questionBE.getStatus());
        question.setUpdated(questionBE.getUpdated());
        Collection<EntityAttribute> attributes = cacheManager.getAllBaseEntityAttributesForBaseEntity(productCode, baseEntityCode);
        updateAttributesInQuestion(question, attributes);
        if(question.getAttribute() == null) {
            log.errorf("Attribute missing for question [%s:%s]", question.getRealm(), question.getAttributeCode());
            // throw new ItemNotFoundException(question.getRealm(), question.getAttributeCode());
        }
        return question;
    }

    public void updateAttributesInQuestion(Question question, Collection<life.genny.qwandaq.attribute.EntityAttribute> entityAttributes) {
        entityAttributes.stream().forEach(entityAttribute -> {
            String columnName = entityAttribute.getAttributeCode();
            log.debugf("Processing attribute %s for question %s", columnName, question.getCode());
            final String attributeValueString = entityAttribute.getValueString();
            switch (columnName) {
                case ATTRIBUTE_CODE -> {
                    log.debugf("Setting attribute for question %s with value %s", question.getCode(), attributeValueString);
                    question.setAttribute(attributeUtils.getAttribute(question.getRealm(), attributeValueString));
                    question.setAttributeCode(attributeValueString);
                }
                case DIRECTIONS -> question.setDirections(attributeValueString);
                case HELPER -> question.setHelper(attributeValueString);
                case HTML -> question.setHtml(attributeValueString);
                case ICON -> question.setIcon(attributeValueString);
                case MANDATORY -> question.setMandatory(entityAttribute.getValueBoolean());
                case ONESHOT -> question.setOneshot(entityAttribute.getValueBoolean());
                case PLACEHOLDER -> question.setPlaceholder(attributeValueString);
                case READONLY -> question.setReadonly(entityAttribute.getValueBoolean());
                default ->
                        log.debugf("Attribute %s not related to 'Question' %s, ignored.", columnName, question.getCode());
            }
        });
    }

    public List<QuestionQuestion> createQuestionQuestionsForParentQuestion(Question parent, Collection<life.genny.qwandaq.attribute.EntityAttribute> entityAttributes) {
        List<QuestionQuestion> questionQuestions = new LinkedList<>();
        String productCode = parent.getRealm();
        entityAttributes.forEach(entityAttribute -> {
            String baseEntityCode = entityAttribute.getBaseEntityCode();
            BaseEntityKey baseEntityKey = new BaseEntityKey(productCode, baseEntityCode);
            QuestionQuestion questionQuestion = getQuestionQuestionInLocalCache(baseEntityKey);
            if (questionQuestion == null) {// && !question.getUpdated().isBefore(baseEntity.getUpdated())) {
                log.debug("Fetching QuesQues -> " + baseEntityCode);
                String[] codes = StringUtils.split(baseEntityCode, '|');
                String childCode = codes[1];
                log.debug("Fetching question for child code -> " + childCode);
                Question child = cacheManager.getQuestionFromBECache(productCode, childCode);
                questionQuestion = new QuestionQuestion(parent, child);
            }
            questionQuestionsLocalCache.put(baseEntityKey, questionQuestion);
            questionQuestions.add(questionQuestion);
        });
        return questionQuestions;
    }

    private QuestionQuestion getQuestionQuestionInLocalCache(BaseEntityKey baseEntityKey) {
        QuestionQuestion questionQuestion = questionQuestionsLocalCache.get(baseEntityKey);
        if (questionQuestion != null) {// && !questionQuestion.getUpdated().isBefore(baseEntity.getUpdated())) {
            return questionQuestion;
        }
        return null;
    }

    public QuestionQuestion getQuestionQuestionFromBaseEntity(life.genny.qwandaq.entity.BaseEntity baseEntity) {
        QuestionQuestion questionQuestion = new QuestionQuestion();
        String beCode = baseEntity.getCode();
        String[] sourceTargetCodes = StringUtils.split(beCode, BaseEntityKey.BE_KEY_DELIMITER);
        questionQuestion.setParentCode(sourceTargetCodes[0]);
        questionQuestion.setChildCode(sourceTargetCodes[1]);
        questionQuestion.setCreated(baseEntity.getCreated());
        String productCode = baseEntity.getRealm();
        questionQuestion.setRealm(productCode);
        questionQuestion.setUpdated(baseEntity.getUpdated());
        Collection<EntityAttribute> attributes = cacheManager.getAllBaseEntityAttributesForBaseEntity(productCode, beCode);
        updateAttributesInQuestionQuestion(questionQuestion, attributes);
        return questionQuestion;
    }

    public void updateAttributesInQuestionQuestion(QuestionQuestion questionQuestion, Collection<EntityAttribute> attributes) {
        attributes.forEach(attribute -> {
            switch (attribute.getAttributeCode()) {
                case SOURCE_CODE -> questionQuestion.setParentCode(attribute.getValueString());
                case TARGET_CODE -> questionQuestion.setChildCode(attribute.getValueString());
                case DISABLED -> questionQuestion.setDisabled(attribute.getValueBoolean());
                case HIDDEN -> questionQuestion.setHidden(attribute.getValueBoolean());
                case ICON -> questionQuestion.setIcon(attribute.getValueString());
                case MANDATORY -> questionQuestion.setMandatory(attribute.getValueBoolean());
                case READONLY -> questionQuestion.setReadonly(attribute.getValueBoolean());
                case VERSION -> questionQuestion.setVersion(attribute.getValueLong());
                case WEIGHT -> questionQuestion.setWeight(attribute.getValueDouble());
                default -> log.debug("Attribute not related to 'QuestionQuestion' entity, ignored.");
            }
        });
    }

    public boolean saveQuestion(Question question) {
        QuestionKey key = new QuestionKey(question.getRealm(), question.getCode());
        return cacheManager.saveEntity(GennyConstants.CACHE_NAME_QUESTION, key, question);
    }

    public boolean saveQuestionQuestion(QuestionQuestion questionQuestion) {
        QuestionQuestionKey key = new QuestionQuestionKey(questionQuestion.getRealm(), questionQuestion.getParentCode(), questionQuestion.getChildCode());
        return cacheManager.saveEntity(GennyConstants.CACHE_NAME_QUESTIONQUESTION, key, questionQuestion);
    }

    public QuestionQuestion findQuestionQuestionBySourceAndTarget(String productCode, String sourceCode, String targetCode) {
        QuestionQuestionKey key = new QuestionQuestionKey(productCode, sourceCode, targetCode);
        return (QuestionQuestion) cacheManager.getPersistableEntity(GennyConstants.CACHE_NAME_QUESTIONQUESTION, key);
    }

    public int removeQuestionQuestion(String productCode, String sourceCode, String targetCode) {
		return cacheManager.removeQuestionQuestion(productCode, sourceCode, targetCode);
    }
}
