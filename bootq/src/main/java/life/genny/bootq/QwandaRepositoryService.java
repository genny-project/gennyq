package life.genny.bootq;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import life.genny.bootq.bootxport.bootx.QwandaRepository;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.validation.Validation;

import javax.persistence.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


// Sync implementation from V7 baseentityService2.java and Service.java
public class QwandaRepositoryService implements QwandaRepository {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().getClass());

    private static final int BATCHSIZE = 500;

    EntityManager em;

    public static final String REALM_HIDDEN = "hidden";
    Map<String, String> ddtCacheMock = new ConcurrentHashMap<>();
    ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    public void writeToDDT(final String key, final String value) {
        ddtCacheMock.put(key, value);
    }

    private String realm;

    protected String getRealm() {

        return realm;
    }

    public QwandaRepositoryService(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return em;
    }


    public void setRealm(String realm) {
        this.realm = realm;
    }

    public <T> void delete(T entity) {
        log.error("line 53 Need implementation");
    }

    public Validation upsert(Validation validation) {
        log.error("line 57 Need implementation");
        return null;
    }

    public Attribute upsert(Attribute attribute) {
        log.error("line 62 Need implementation");
        return null;
    }

    public BaseEntity upsert(BaseEntity baseEntity) {
        log.error("line 67 Need implementation");
        return null;
    }

    public Question upsert(Question q) {
        log.error("line 72 Need implementation");
        return null;
    }

    public Question upsert(Question q, HashMap<String, Question> mapping) {
        try {
            String code = q.getCode();
            Question val = mapping.get(code);
            BeanNotNullFields copyFields = new BeanNotNullFields();
            if (val == null) {
                throw new NoResultException();
            }
            copyFields.copyProperties(val, q);

            val.setRealm(getRealm());
            Set<ConstraintViolation<Question>> constraints = validator.validate(val);
            for (ConstraintViolation<Question> constraint : constraints) {
                log.error(constraint.getPropertyPath() + " " + constraint.getMessage());
            }
            if (constraints.isEmpty()) {
                val = getEntityManager().merge(val);
                return val;
            } else {
                log.error("Error in Hibernate Validation for quesiton " + q.getCode() + " with attribute code :" + q.getAttributeCode());
            }
            return null; // TODO throw an error

        } catch (NoResultException | IllegalAccessException | InvocationTargetException e) {
            try {

                q.setRealm(getRealm());
                if (BatchLoading.isSynchronise()) {
                    Question val = findQuestionByCode(q.getCode(), REALM_HIDDEN);
                    if (val != null) {
                        val.setRealm(getRealm());
                        updateRealm(val);
                        return val;
                    }
                }

                getEntityManager().persist(q);
            } catch (javax.validation.ConstraintViolationException ce) {
                log.error("Error in saving question due to constraint issue:" + q + " :" + ce.getLocalizedMessage());
            } catch (javax.persistence.PersistenceException pe) {
                log.error("Error in saving question :" + q + " :" + pe.getLocalizedMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Long id = q.getId();
            return q;
        }
    }

    public Long insert(Ask ask) {
        log.error("line 82 Need implementation");
        return null;
    }

    public Validation findValidationByCode(@NotNull String code) {
        return findValidationByCode(code, getRealm());
    }

    public Validation findValidationByCode(@NotNull final String code, @NotNull final String realm)
            throws NoResultException {
        Validation result = null;
        try {
            result = (Validation) getEntityManager()
                    .createQuery("SELECT a FROM Validation a where a.code=:code and a.realm=:realmStr")
                    .setParameter("realmStr", realm).setParameter("code", code).getSingleResult();
        } catch (Exception e) {
            return null;
        }

        return result;
    }

    public Attribute findAttributeByCode(@NotNull String code) {
        log.error("line 92 Need implementation");
        return null;
    }

    public BaseEntity findBaseEntityByCode(@NotNull String baseEntityCode) {
        log.error("line 97 Need implementation");
        return null;
    }

    public Long updateWithAttributes(BaseEntity entity) {
        entity.setRealm(getRealm());

        try {
            // merge in entityAttributes
            entity = getEntityManager().merge(entity);
        } catch (final Exception e) {
            // so persist otherwise
            if (entity.getName() == null) {
                entity.setName(entity.getCode());
            }
            getEntityManager().persist(entity);
        }
        String json = JsonUtils.toJson(entity);
        writeToDDT(entity.getCode(), json);
        return entity.getId();
    }

    public void bulkUpdateWithAttributes(List<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            updateWithAttributes(entity);
        }
    }

    public EntityEntity findEntityEntity(String sourceCode, String targetCode, String linkCode) {
        log.error("line 127 Need implementation");
        return null;
    }

    public Integer updateEntityEntity(EntityEntity ee) {
        Integer result = 0;

        try {
            String sql = "update EntityEntity ee set ee.weight=:weight, ee.valueString=:valueString, ee.link.weight=:weight, ee.link.linkValue=:valueString where ee.pk.targetCode=:targetCode and ee.link.attributeCode=:linkAttributeCode and ee.link.sourceCode=:sourceCode";
            result = getEntityManager().createQuery(sql).setParameter("sourceCode", ee.getPk().getSource().getCode())
                    .setParameter("linkAttributeCode", ee.getLink().getAttributeCode())
                    .setParameter("targetCode", ee.getPk().getTargetCode()).setParameter("weight", ee.getWeight())
                    .setParameter("valueString", ee.getValueString()).executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    public EntityEntity insertEntityEntity(EntityEntity ee) {
        try {
            getEntityManager().persist(ee);
        } catch (Exception e) {
            log.error("Get Exception:" + e.getMessage() + " when insertEntityEntity");
        }
        return ee;
    }

    public QuestionQuestion findQuestionQuestionByCode(String sourceCode, String targetCode) {
        log.error("line 164 Need implementation");
        return null;
    }

    public Question findQuestionByCode(@NotNull String code) {
        List<Question> result = null;
        final String userRealmStr = getRealm();
        try {

            result = getEntityManager().createQuery("SELECT a FROM Question a where a.code=:code and a.realm=:realmStr")

                    .setParameter("realmStr", getRealm()).setParameter("code", code.toUpperCase()).getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    public QuestionQuestion upsert(QuestionQuestion qq) {
        log.error("line 174 Need implementation");
        return null;
    }

    public Question findQuestionByCode(@NotNull String code, @NotNull String realm) {
        List<Question> result = null;
        try {
            result = getEntityManager().createQuery("SELECT a FROM Question a where a.code=:code and a.realm=:realmStr")
                    .setParameter("realmStr", realm).setParameter("code", code.toUpperCase()).getResultList();

        } catch (Exception e) {
            return null;
        }
        return result.get(0);
    }

    public Long updateRealm(Question que) {
        Long result = 0L;

        try {
            result = (long) getEntityManager()
                    .createQuery("update Question que set que.realm =:realm where que.code=:code")
                    .setParameter("code", que.getCode()).setParameter("realm", que.getRealm()).executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Long insert(final Question question) {
        // always check if question exists through check for unique code
        try {

            question.setRealm(getRealm());

            getEntityManager().persist(question);
            log.debug("Loaded " + question.getCode());
        } catch (final ConstraintViolationException e) {
            Question existing = findQuestionByCode(question.getCode());

            existing.setRealm(getRealm());

            existing = getEntityManager().merge(existing);
            return existing.getId();
        } catch (final PersistenceException e) {
            Question existing = findQuestionByCode(question.getCode());

            existing.setRealm(getRealm());

            existing = getEntityManager().merge(existing);
            return existing.getId();
        } catch (final IllegalStateException e) {
            Question existing = findQuestionByCode(question.getCode());

            existing.setRealm(getRealm());

            existing = getEntityManager().merge(existing);
            return existing.getId();
        } catch (Exception ex) {
            log.error("Invalid questionCode:" + question.getCode() + ", errorMsg:" + ex.getMessage());
            throw ex;
        }
        return question.getId();
    }

    public <T> List<T> queryTableByRealm(String tableName, String realm) {
        List<T> result = Collections.emptyList();
        try {
            Query query = getEntityManager().createQuery(String.format("SELECT temp FROM %s temp where temp.realm=:realmStr", tableName));
            query.setParameter("realmStr", realm);
            result = query.getResultList();
        } catch (Exception e) {
            log.error(String.format("Query table %s Error:%s".format(realm, e.getMessage())));
        }
        return result;
    }

    public void bulkUpdate(ArrayList<CodedEntity> objectList, HashMap<String, CodedEntity> mapping) {
        if (objectList.isEmpty()) return;

        BeanNotNullFields copyFields = new BeanNotNullFields();
        for (CodedEntity t : objectList) {
            CodedEntity val = mapping.get(t.getCode());
            if (val == null) {
                // Should never raise this exception
                throw new NoResultException(String.format("Can't find %s from database.", t.getCode()));
            }
            try {
                copyFields.copyProperties(val, t);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                log.error(String.format("Failed to copy Properties for %s", val.getCode()));
            }

            val.setRealm(getRealm());
            getEntityManager().merge(val);
        }
    }

    public void bulkInsert(ArrayList<CodedEntity> objectList) {
        if (objectList.isEmpty()) return;

        EntityManager em1 = getEntityManager();
        int index = 1;

        for (CodedEntity t : objectList) {
            em1.persist(t);
            if (index % BATCHSIZE == 0) {
                //flush a batch of inserts and release memory:
                log.debug("BaseEntity Batch is full, flush to database.");
                em1.flush();
            }
            index += 1;
        }
        em1.flush();
    }

    public void bulkInsertAsk(ArrayList<Ask> objectList) {
        log.error(" line 296 Need implementation");
    }

    public void bulkUpdateAsk(ArrayList<Ask> objectList, HashMap<String, Ask> mapping) {
        log.error("line 300 Need implementation");
    }

    public void bulkInsertQuestionQuestion(ArrayList<QuestionQuestion> objectList) {
        if (objectList.isEmpty()) return;

        EntityManager entityManager = getEntityManager();
        int index = 1;

        for (QuestionQuestion t : objectList) {
            entityManager.persist(t);
            if (index % BATCHSIZE == 0) {
                //flush a batch of inserts and release memory:
                log.debug("BaseEntity Batch is full, flush to database.");
                entityManager.flush();
            }
            index += 1;
        }
        entityManager.flush();
    }

    public void bulkUpdateQuestionQuestion(ArrayList<QuestionQuestion> objectList, HashMap<String, QuestionQuestion> mapping) {
        for (QuestionQuestion qq : objectList) {
            String uniqCode = qq.getSourceCode() + "-" + qq.getTarketCode();
            QuestionQuestion existing = mapping.get(uniqCode.toUpperCase());
            existing.setMandatory(qq.getMandatory());
            existing.setWeight(qq.getWeight());
            existing.setReadonly(qq.getReadonly());
            existing.setDependency(qq.getDependency());
            existing.setIcon(qq.getIcon());
            getEntityManager().merge(existing);
        }
    }

    public void cleanAsk(String realm) {
        String qlString = String.format("delete from ask where realm = '%s'", realm);
        EntityManager em1 = getEntityManager();
        Query query = em1.createNativeQuery(qlString);
        int number = query.executeUpdate();
        em1.flush();
        log.info(String.format("Clean up ask, realm:%s, %d ask deleted", realm, number));
    }

    public void cleanFrameFromBaseentityAttribute(String realm) {
        String qlString = "delete from baseentity_attribute " +
                "where baseEntityCode like \'RUL_FRM%_GRP\' " +
                "and attributeCode = \'PRI_ASKS\' " +
                "and realm = \'" + realm + "\'";
        EntityManager em1 = getEntityManager();
        Query query = em1.createNativeQuery(qlString);
        int number = query.executeUpdate();
        em1.flush();
        log.info(String.format("Clean up BaseentityAttribute, realm:%s, %d BaseentityAttribute deleted", realm, number));
    }

    public List<Attribute> findAttributes() throws NoResultException {

        final List<Attribute> results = getEntityManager()
                .createQuery("SELECT a FROM Attribute a where a.realm=:realmStr").setParameter("realmStr", getRealm())
                .getResultList();

        return results;
    }
}
