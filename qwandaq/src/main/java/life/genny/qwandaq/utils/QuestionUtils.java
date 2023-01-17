package life.genny.qwandaq.utils;

import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.serialization.baseentity.BaseEntity;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class QuestionUtils {

    private static final Logger log = Logger.getLogger(QuestionUtils.class);
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

    public life.genny.qwandaq.entity.BaseEntity getPersistableBaseEntityFromQuestion(Question question) {
        life.genny.qwandaq.entity.BaseEntity baseEntity = (life.genny.qwandaq.entity.BaseEntity) getSerializableBaseEntityFromQuestion(question).toPersistableCoreEntity();
        List<EntityAttribute> persistableBaseEntityAttributes = new LinkedList<>();
        List<BaseEntityAttribute> serializableBaseEntityAttributes = getSerializableBaseEntityAttributesFromQuestion(question);
        serializableBaseEntityAttributes.forEach(baseEntityAttribute -> persistableBaseEntityAttributes.add((EntityAttribute) baseEntityAttribute.toPersistableCoreEntity()));
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

    public List<BaseEntityAttribute> getSerializableBaseEntityAttributesFromQuestion(Question question) {
        List<BaseEntityAttribute> attributes = new LinkedList<>();
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

    public BaseEntityAttribute createSerializableBaseEntityAttributeFromQuestion(Question question, String attributeCode, Object value) {
        BaseEntityAttribute attribute = new BaseEntityAttribute();
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
            attribute.setMoney((Money) value);
        } else {
            attribute.setValueString(value.toString());
        }
        return attribute;
    }

    public life.genny.qwandaq.entity.BaseEntity getPersistableBaseEntityFromQuestionQuestion(QuestionQuestion questionQuestion) {
        life.genny.qwandaq.entity.BaseEntity baseEntity = (life.genny.qwandaq.entity.BaseEntity) getSerializableBaseEntityFromQuestionQuestion(questionQuestion).toPersistableCoreEntity();
        List<EntityAttribute> persistableBaseEntityAttributes = new LinkedList<>();
        List<BaseEntityAttribute> serializableBaseEntityAttributes = getSerializableBaseEntityAttributesFromQuestionQuestion(questionQuestion);
        serializableBaseEntityAttributes.forEach(baseEntityAttribute -> persistableBaseEntityAttributes.add((EntityAttribute) baseEntityAttribute.toPersistableCoreEntity()));
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

    public List<BaseEntityAttribute> getSerializableBaseEntityAttributesFromQuestionQuestion(QuestionQuestion questionQuestion) {
        List<BaseEntityAttribute> attributes = new LinkedList<>();
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

    public BaseEntityAttribute createSerializableBaseEntityAttributeFromQuestionQuestion(QuestionQuestion questionQuestion, String attributeCode, Object value) {
        BaseEntityAttribute attribute = new BaseEntityAttribute();
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
            attribute.setMoney((Money) value);
        } else {
            attribute.setValueString(value.toString());
        }
        return attribute;
    }

    public Question getQuestionFromBaseEntity(life.genny.qwandaq.entity.BaseEntity baseEntity, Set<EntityAttribute> attributes) {
        Question question = new Question();
		log.info("Question Code From BaseEntity = " + baseEntity.getCode());
        question.setCode(baseEntity.getCode());
        question.setCreated(baseEntity.getCreated());
        question.setName(baseEntity.getName());
        question.setRealm(baseEntity.getRealm());
        question.setStatus(baseEntity.getStatus());
        question.setUpdated(baseEntity.getUpdated());
        updateAttributesInQuestion(question, attributes);
        if(question.getAttribute() == null) {
            log.errorf("Attribute missing for question [%s:%s]", question.getRealm(), question.getAttributeCode());
            // throw new ItemNotFoundException(question.getRealm(), question.getAttributeCode());
        }
        return question;
    }

    public void updateAttributesInQuestion(Question question, Set<EntityAttribute> entityAttributes) {
        entityAttributes.stream().forEach(entityAttribute -> {
            String columnName = entityAttribute.getAttributeCode();
            log.debugf("Processing attribute %s for question %s", columnName, question.getCode());
            switch (columnName) {
                case ATTRIBUTE_CODE -> {
                    log.debugf("Setting attribute for question %s with value %s", question.getCode(), entityAttribute.getValueString());
                    question.setAttribute(entityAttribute.getAttribute());
                    question.setAttributeCode(entityAttribute.getValueString());
                }
                case DIRECTIONS -> question.setDirections(entityAttribute.getValueString());
                case HELPER -> question.setHelper(entityAttribute.getValueString());
                case HTML -> question.setHtml(entityAttribute.getValueString());
                case ICON -> question.setIcon(entityAttribute.getValueString());
                case MANDATORY -> question.setMandatory(entityAttribute.getValueBoolean());
                case ONESHOT -> question.setOneshot(entityAttribute.getValueBoolean());
                case PLACEHOLDER -> question.setPlaceholder(entityAttribute.getValueString());
                case READONLY -> question.setReadonly(entityAttribute.getValueBoolean());
                default ->
                        log.debugf("Attribute %s not related to 'Question' %s, ignored.", columnName, question.getCode());
            }
        });
    }

    public QuestionQuestion getQuestionQuestionFromBaseEntityBaseEntityAttributes(life.genny.qwandaq.entity.BaseEntity baseEntity, Set<EntityAttribute> attributes) {
        QuestionQuestion questionQuestion = new QuestionQuestion();
        String beCode = baseEntity.getCode();
        String[] sourceTargetCodes = beCode.split(BaseEntityKey.BE_KEY_DELIMITER);
        questionQuestion.setParentCode(sourceTargetCodes[0]);
        questionQuestion.setChildCode(sourceTargetCodes[1]);
        questionQuestion.setCreated(baseEntity.getCreated());
        questionQuestion.setRealm(baseEntity.getRealm());
        questionQuestion.setUpdated(baseEntity.getUpdated());
        updateAttributesInQuestionQuestion(questionQuestion, attributes);
        return questionQuestion;
    }

    public void updateAttributesInQuestionQuestion(QuestionQuestion questionQuestion, Set<EntityAttribute> attributes) {
        attributes.parallelStream().forEach(attribute -> {
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
}
