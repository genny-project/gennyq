package life.genny.bootq.bootxport.bootx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotNull;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.EntityEntity;
import life.genny.qwandaq.validation.Validation;


public interface QwandaRepository {

    void setRealm(String realm);

    <T> void delete(T entity);

    Validation upsert(Validation validation);

    Attribute upsert(Attribute attribute);

    BaseEntity upsert(BaseEntity baseEntity);

    Question upsert(Question q);

    Question upsert(Question q, HashMap<String, Question> mapping);

    Long insert(final Ask ask);

    Validation findValidationByCode(@NotNull final String code);

    Attribute findAttributeByCode(@NotNull final String code);

    BaseEntity findBaseEntityByCode(@NotNull final String baseEntityCode);

    Long updateWithAttributes(BaseEntity entity);

    void bulkUpdateWithAttributes(List<BaseEntity> entities);

    EntityEntity findEntityEntity(final String sourceCode, final String targetCode, final String linkCode);

    Integer updateEntityEntity(final EntityEntity ee);

    EntityEntity insertEntityEntity(final EntityEntity ee);

    QuestionQuestion findQuestionQuestionByCode(
            final String sourceCode, final String targetCode);

    Question findQuestionByCode(@NotNull final String code);

    QuestionQuestion upsert(QuestionQuestion qq);

    Question findQuestionByCode(@NotNull final String code, @NotNull final String realm);

    Long updateRealm(Question que);

    Long insert(final Question question);

    <T> List<T> queryTableByRealm(String tableName, String realm);

    // For Validation, Attribute, AttributeLink, QuestionQuestion, QBaseMSGMessageTemplate
    void bulkUpdate(ArrayList<CodedEntity> objectList, HashMap<String, CodedEntity> mapping);

    void bulkInsert(ArrayList<CodedEntity> objectList);

    void bulkInsertAsk(ArrayList<Ask> objectList);

    void bulkUpdateAsk(ArrayList<Ask> objectList, HashMap<String, Ask> mapping);

    void bulkInsertQuestionQuestion(ArrayList<QuestionQuestion> objectList);

    void bulkUpdateQuestionQuestion(ArrayList<QuestionQuestion> objectList, HashMap<String, QuestionQuestion> mapping);

    void cleanAsk(String realm);
    void cleanFrameFromBaseentityAttribute(String realm);
}
