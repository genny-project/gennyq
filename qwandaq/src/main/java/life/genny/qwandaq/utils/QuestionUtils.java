package life.genny.qwandaq.utils;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.serialization.baseentity.BaseEntity;
import life.genny.qwandaq.serialization.baseentity.BaseEntityKey;
import life.genny.qwandaq.serialization.baseentityattribute.BaseEntityAttribute;
import org.javamoney.moneta.Money;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class QuestionUtils {

    private static final Logger log = Logger.getLogger(QuestionUtils.class);

    public BaseEntity getBaseEntityFromQuestion(Question question) {
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.setCode(question.getCode());
        baseEntity.setCreated(question.getCreated());
        baseEntity.setName(question.getName());
        baseEntity.setRealm(question.getRealm());
        baseEntity.setStatus(question.getStatus().ordinal());
        baseEntity.setUpdated(question.getUpdated());
        return baseEntity;
    }

    public List<BaseEntityAttribute> getBaseEntityAttributesFromQuestion(Question question) {
        List<BaseEntityAttribute> attributes = new LinkedList<>();
        attributes.add(createBaseEntityAttributeFromQuestion(question, "attributeCode", question.getAttributeCode()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "directions", question.getDirections()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "helper", question.getHelper()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "html", question.getHtml()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "icon", question.getIcon()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "mandatory", question.getMandatory()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "oneshot", question.getOneshot()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "placeholder", question.getPlaceholder()));
        attributes.add(createBaseEntityAttributeFromQuestion(question, "readonly", question.getReadonly()));
        return attributes;
    }

    public BaseEntityAttribute createBaseEntityAttributeFromQuestion(Question question, String attributeCode, Object value) {
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

    public BaseEntity getBaseEntityFromQuestionQuestion(QuestionQuestion questionQuestion) {
        BaseEntity baseEntity = new BaseEntity();
        baseEntity.setCode(questionQuestion.getParentCode() + BaseEntityKey.BE_KEY_DELIMITER + questionQuestion.getChildCode());
        baseEntity.setCreated(questionQuestion.getCreated());
        baseEntity.setRealm(questionQuestion.getRealm());
        baseEntity.setUpdated(questionQuestion.getUpdated());
        return baseEntity;
    }

    public List<BaseEntityAttribute> getBaseEntityAttributesFromQuestionQuestion(QuestionQuestion questionQuestion) {
        List<BaseEntityAttribute> attributes = new LinkedList<>();
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "parentCode", questionQuestion.getParentCode()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "childCode", questionQuestion.getChildCode()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "createOnTrigger", questionQuestion.getCreateOnTrigger()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "dependency", questionQuestion.getDependency()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "disabled", questionQuestion.getDisabled()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "formTrigger", questionQuestion.getFormTrigger()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "hidden", questionQuestion.getHidden()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "icon", questionQuestion.getIcon()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "mandatory", questionQuestion.getMandatory()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "oneshot", questionQuestion.getOneshot()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "readonly", questionQuestion.getReadonly()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "version", questionQuestion.getVersion()));
        attributes.add(createBaseEntityAttributeFromQuestionQuestion(questionQuestion, "weight", questionQuestion.getWeight()));
        return attributes;
    }

    public BaseEntityAttribute createBaseEntityAttributeFromQuestionQuestion(QuestionQuestion questionQuestion, String attributeCode, Object value) {
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

    public Question getQuestionFromSerializableBaseEntity(BaseEntity baseEntity, Set<BaseEntityAttribute> attributes) {
        Question question = new Question();
        question.setCode(baseEntity.getCode());
        question.setCreated(baseEntity.getCreated());
        question.setName(baseEntity.getName());
        question.setRealm(baseEntity.getRealm());
        question.setStatus(EEntityStatus.valueOf(baseEntity.getStatus()));
        question.setUpdated(baseEntity.getUpdated());
        updateAttributesInQuestion(question, attributes);
        return question;
    }

    public void updateAttributesInQuestion(Question question, Set<BaseEntityAttribute> attributes) {
        attributes.parallelStream().forEach(attribute -> {
            switch(attribute.getAttributeCode()) {
                case "attributeCode":
                    question.setAttributeCode(attribute.getValueString());
                    break;
                case "directions":
                    question.setDirections(attribute.getValueString());
                    break;
                case "helper":
                    question.setHelper(attribute.getValueString());
                    break;
                case "html":
                    question.setHtml(attribute.getValueString());
                    break;
                case "icon":
                    question.setIcon(attribute.getValueString());
                    break;
                case "mandatory":
                    question.setMandatory(attribute.getValueBoolean());
                    break;
                case "oneshot":
                    question.setOneshot(attribute.getValueBoolean());
                    break;
                case "placeholder":
                    question.setPlaceholder(attribute.getValueString());
                    break;
                case "readonly":
                    question.setReadonly(attribute.getValueBoolean());
                    break;
                default:
                    log.debug("Attribute not related to 'Question' entity, ignored.");
                    break;
            }
        });
    }

    public QuestionQuestion getQuestionQuestionFromBaseEntityBaseEntityAttributes(BaseEntity baseEntity, Set<BaseEntityAttribute> attributes) {
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

    public void updateAttributesInQuestionQuestion(QuestionQuestion questionQuestion, Set<BaseEntityAttribute> attributes) {
        attributes.parallelStream().forEach(attribute -> {
            switch (attribute.getAttributeCode()) {
                case "sourceCode":
                    questionQuestion.setParentCode(attribute.getValueString());
                    break;
                case "targetCode":
                    questionQuestion.setChildCode(attribute.getValueString());
                    break;
                case "createOnTrigger":
                    questionQuestion.setCreateOnTrigger(attribute.getValueBoolean());
                    break;
                case "dependency":
                    questionQuestion.setDependency(attribute.getValueString());
                    break;
                case "disabled":
                    questionQuestion.setDisabled(attribute.getValueBoolean());
                    break;
                case "formTrigger":
                    questionQuestion.setFormTrigger(attribute.getValueBoolean());
                    break;
                case "hidden":
                    questionQuestion.setHidden(attribute.getValueBoolean());
                    break;
                case "icon":
                    questionQuestion.setIcon(attribute.getValueString());
                    break;
                case "mandatory":
                    questionQuestion.setMandatory(attribute.getValueBoolean());
                    break;
                case "oneshot":
                    questionQuestion.setOneshot(attribute.getValueBoolean());
                    break;
                case "readonly":
                    questionQuestion.setReadonly(attribute.getValueBoolean());
                    break;
                case "version":
                    questionQuestion.setVersion(attribute.getValueLong());
                    break;
                case "weight":
                    questionQuestion.setWeight(attribute.getValueDouble());
                    break;
                default:
                    log.debug("Attribute not related to 'QuestionQuestion' entity, ignored.");
                    break;
            }
        });
    }
}
