package life.genny.qwandaq.utils;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.HBaseEntity;
import org.jboss.logging.Logger;

import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.validation.Validation;

/*
 * A utility class used for standard read and write 
 * operations to the database.
 * 
 * @author Jasper Robison
 * @author Bryn Mecheam
 */
@ApplicationScoped
public class DatabaseUtils {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	EntityManager entityManager;

	/**
	 * Get all attributes with a specific Prefix
	 * 
	 * @param productCode - Product Code to load from
	 * @param prefix      - Prefix to check for
	 * @return A list of attributes
	 */
	public List<Attribute> findAttributesWithPrefix(String productCode, String prefix) {
		return findAttributes(productCode, null, null, prefix);
	}

	/**
	 * Fetch Validations from the database using page size and num.
	 * If pageSize and pageNumber are both null, all results will be returned at
	 * once.
	 * If wildcard is not null, the result codes will contain the wildcard string.
	 * 
	 * @param realm      the realm to find in
	 * @param pageSize   the pageSize to fetch
	 * @param pageNumber the pageNumber to fetch
	 * @param wildcard   perform a wildcard on the code field
	 * @return List
	 */
	public List<Validation> findValidations(String realm, Integer pageSize, Integer pageNumber,
			String wildcard) {

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());
		String queryStr = "FROM Validation WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");
		Query query = entityManager.createQuery(queryStr, Validation.class)
				.setParameter("realmStr", realm);

		if (isWildcard) {
			query.setParameter("code", "%" + wildcard + "%");
		}
		if (pageNumber != null && pageSize != null) {
			query = query.setFirstResult((pageNumber - 1) * pageSize)
					.setMaxResults(pageSize);
		}

		return query.getResultList();
	}

	/**
	 * Count the number of attributes in a realm database.
	 * 
	 * @param realm The realm to query on
	 * @return A Long representing the number of attributes
	 */
	public Long countAttributes(String realm) {

		return (Long) entityManager
				.createQuery("SELECT count(1) FROM Attribute WHERE realm=:realmStr AND name not like 'App\\_%'")
				.setParameter("realmStr", realm)
				.getResultList().get(0);
	}

	/**
	 * Fetch Attributes from the database using page size and num.
	 * If pageSize and pageNumber are both null, all results will be returned at
	 * once.
	 * If wildcard is not null, the result codes will contain the wildcard string.
	 * 
	 * @param realm    the realm to find in
	 * @param startIdx the start index to fetch
	 * @param pageSize the pageSize to fetch (Starting from Page 1)
	 * @param wildcard perform a wildcard on the code field
	 * @return List
	 */
	public List<Attribute> findAttributes(String realm, Integer startIdx, Integer pageSize, String wildcard) {

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());
		String queryStr = "FROM Attribute WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "")
				+ " AND name not like 'App\\_%' order by id";

		Query query = entityManager.createQuery(queryStr, Attribute.class)
				.setParameter("realmStr", realm);

		if (isWildcard) {
			query.setParameter("code", "%" + wildcard + "%");
		}
		if ((startIdx == null || startIdx == 0) && (pageSize == null || pageSize == 0)) {
			log.info("Fetching all Attributes (unset pageNumber or pageSize)");
		} else {
			query = query.setFirstResult(startIdx).setMaxResults(pageSize);
		}

		return query.getResultList();
	}

	/**
	 * Fetch a list of {@link BaseEntity} types from the database using a realm.
	 * If pageSize and pageNumber are both null, all results will be returned at
	 * once.
	 * If wildcard is not null, the result codes will contain the wildcard string.
	 * 
	 * @param realm      the realm to find in
	 * @param pageSize   the pageSize to fetch
	 * @param pageNumber the pageNumber to fetch
	 * @param wildcard   perform a wildcard on the code field
	 * @return List
	 */
	public List<BaseEntity> findBaseEntitys(String realm, Integer pageSize, Integer pageNumber,
											String wildcard) {

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());
		String queryStr = "FROM HBaseEntity WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");
		Query query = entityManager.createQuery(queryStr, HBaseEntity.class)
				.setParameter("realmStr", realm);

		if (isWildcard) {
			query.setParameter("code", "%" + wildcard + "%");
		}
		if (pageNumber != null && pageSize != null) {
			query = query.setFirstResult((pageNumber - 1) * pageSize)
					.setMaxResults(pageSize);
		}

		List<BaseEntity> baseEntities = new LinkedList<>();
		List<HBaseEntity> hBaseEntities = query.getResultList();
		hBaseEntities.stream().forEach(hbe -> baseEntities.add(hbe.toBaseEntity()));
		return baseEntities;
	}

	/**
	 * Fetch a list of {@link Question} types from the database using a realm, page
	 * size and page number.
	 * If pageSize and pageNumber are both null, all results will be returned at
	 * once.
	 * If wildcard is not null, the result codes will contain the wildcard string.
	 * 
	 * @param realm      the realm to find in
	 * @param pageSize   the pageSize to fetch
	 * @param pageNumber the pageNumber to fetch
	 * @param wildcard   perform a wildcard on the code field
	 * @return List
	 */
	public List<Question> findQuestions(String realm, Integer pageSize, Integer pageNumber, String wildcard) {

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());
		String queryStr = "FROM Question WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");
		Query query = entityManager.createQuery(queryStr, Question.class)
				.setParameter("realmStr", realm);

		if (isWildcard) {
			query.setParameter("code", "%" + wildcard + "%");
		}
		if (pageNumber != null && pageSize != null) {
			query = query.setFirstResult((pageNumber - 1) * pageSize)
					.setMaxResults(pageSize);
		}

		return query.getResultList();
	}

	/**
	 * Fetch a list of {@link QuestionQuestion} types from the database using a
	 * realm, page size and page number.
	 * If pageSize and pageNumber are both null, all results will be returned at
	 * once.
	 * If wildcard is not null, the result sourceCodes will contain the wildcard
	 * string.
	 * 
	 * @param realm      the realm to find in
	 * @param pageSize   the pageSize to fetch
	 * @param pageNumber the pageNumber to fetch
	 * @param wildcard   perform a wildcard on the code field
	 * @return List
	 */
	public List<QuestionQuestion> findQuestionQuestions(String realm, Integer pageSize, Integer pageNumber,
			String wildcard) {

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());
		String queryStr = "FROM QuestionQuestion WHERE realm=:realmStr"
				+ (isWildcard ? " AND sourceCode like :code" : "");
		Query query = entityManager.createQuery(queryStr, QuestionQuestion.class)
				.setParameter("realmStr", realm);

		if (isWildcard) {
			query.setParameter("code", "%" + wildcard + "%");
		}
		if (pageNumber != null && pageSize != null) {
			query = query.setFirstResult((pageNumber - 1) * pageSize)
					.setMaxResults(pageSize);
		}

		return query.getResultList();
	}

	/**
	 * Grab a Validation from the database using a code and a realm.
	 * 
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Validation
	 */

	public Validation findValidationByCode(String realm, String code) {

		return entityManager
				.createQuery("FROM Validation WHERE realm=:realmStr AND code=:code", Validation.class)
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.getSingleResult();
	}

	/**
	 * Fetch an Attribute from the database using a realm and a code.
	 * 
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Attribute
	 */

	public Attribute findAttributeByCode(String realm, String code) {

		return entityManager
				.createQuery("FROM Attribute WHERE realm=:realmStr AND code =:code", Attribute.class)
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.getSingleResult();
	}

	/**
	 * Fetch A {@link BaseEntity} from the database using the entity code.
	 *
	 * @param realm The realm that the {@link BaseEntity} is saved under
	 * @param code  The code of the {@link BaseEntity} to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity findBaseEntityByCode(String realm, String code) {

		return entityManager
				.createQuery("FROM HBaseEntity WHERE realm=:realmStr AND code=:code", HBaseEntity.class)
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.getSingleResult().toBaseEntity();
	}

	/**
	 * Fetch A {@link Question} from the database using the question code.
	 * 
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Question
	 */

	public Question findQuestionByCode(String realm, String code) {

		return entityManager
				.createQuery("FROM Question WHERE realm=:realmStr AND code=:code", Question.class)
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.getSingleResult();
	}

	/**
	 * Find a QuestionQuestion using a realm, a sourceCode and a targetCode.
	 * 
	 * @param realm      the realm to find in
	 * @param sourceCode the sourceCode to find by
	 * @param targetCode the targetCode to find by
	 * @return List list of QuestionQuestions
	 */

	public QuestionQuestion findQuestionQuestionBySourceAndTarget(String realm,
			String sourceCode, String targetCode) {

		return entityManager
				.createQuery(
						"FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode AND targetCode = :targetCode",
						QuestionQuestion.class)
				.setParameter("realmStr", realm)
				.setParameter("sourceCode", sourceCode)
				.setParameter("targetCode", targetCode)
				.getSingleResult();
	}

	/**
	 * Find a list of QuestionQuestions using a realm and a sourceCode
	 * 
	 * @param realm      the realm to find in
	 * @param sourceCode the sourceCode to find by
	 * @return List list of QuestionQuestions
	 */
	public List<QuestionQuestion> findQuestionQuestionsBySourceCode(String realm, String sourceCode) {

		return entityManager
				.createQuery(
						"FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode order by weight ASC",
						QuestionQuestion.class)
				.setParameter("realmStr", realm)
				.setParameter("sourceCode", sourceCode)
				.getResultList();
	}

	/**
	 * Find a list of Asks using question code, sourceCode and targetCode.
	 * 
	 * @param realm        realm to find in
	 * @param questionCode questionCode to find
	 * @param sourceCode   sourceCode to find by
	 * @param targetCode   targetCode to find by
	 * @return List list of asks
	 */
	public List<Ask> findAsksByQuestionCode(String realm, String questionCode,
			String sourceCode, String targetCode) {

		return entityManager
				.createQuery("FROM Ask WHERE realm=:realmStr AND sourceCode=:sourceCode"
						+ " AND targetCode=:targetCode AND questionCode=:questionCode", Ask.class)
				.setParameter("questionCode", questionCode)
				.setParameter("sourceCode", sourceCode)
				.setParameter("realmStr", realm)
				.setParameter("targetCode", targetCode)
				.getResultList();
	}

	/**
	 * Find the parent links.
	 * 
	 * @param realm      Realm to query
	 * @param targetCode Code of the target entity
	 * @return A list of Links
	 */
	public List<Link> findParentLinks(String realm, String targetCode) {

		return entityManager.createQuery("SELECT ee.link FROM EntityEntity ee"
				+ " where ee.pk.targetCode=:targetCode and ee.pk.source.realm=:realmStr", Link.class)
				.setParameter("targetCode", targetCode)
				.setParameter("realmStr", realm)
				.getResultList();
	}

	/**
	 * Save a {@link Validation} to the database.
	 * 
	 * @param validation A {@link Validation} object to save
	 */
	@Transactional
	public void saveValidation(Validation validation) {

		log.info("Saving Validation " + validation.getCode());

		Validation existingValidation = null;
		try {
			existingValidation = findValidationByCode(validation.getRealm(), validation.getCode());
		} catch (NoResultException e) {
			log.debugf("%s not found in database, creating new row...", validation.getCode());
		}

		if (existingValidation == null) {
			entityManager.persist(validation);
		} else {
			entityManager.merge(validation);
		}
		log.info("Successfully saved Validation " + validation.getCode());
	}

	/**
	 * Save an {@link Attribute} to the database.
	 * 
	 * @param attribute An {@link Attribute} object to save
	 */
	@Transactional
	public void saveAttribute(Attribute attribute) {

		log.info("Saving Attribute " + attribute.getCode());

		Attribute existingAttribute = null;
		try {
			existingAttribute = findAttributeByCode(attribute.getRealm(), attribute.getCode());
		} catch (NoResultException e) {
			log.debugf("%s not found in database, creating new row...", attribute.getCode());
		}

		if (existingAttribute == null) {
			entityManager.persist(attribute);
		} else {
			entityManager.merge(attribute);
		}
		log.info("Successfully saved Attribute " + attribute.getCode());
	}

	/**
	 * Save a {@link BaseEntity} to the database.
	 * 
	 * @param entity A {@link BaseEntity} object to save
	 */
	@Transactional
	public void saveBaseEntity(BaseEntity entity) {

		log.debug("Saving BaseEntity " + entity.getRealm() + ":" + entity.getCode());

		BaseEntity existingEntity = null;
		try {
			existingEntity = findBaseEntityByCode(entity.getRealm(), entity.getCode());
		} catch (NoResultException e) {
			log.debugf("%s not found in database, creating new row...", entity.getCode());
		}

		if (existingEntity == null) {
			log.debug("New BaseEntity being saved to DB -> " + entity.getCode());
			entityManager.persist(entity.toHBaseEntity());
		} else {
			if (entity.getId() == null)
				entity.setId(existingEntity.getId());
			entityManager.merge(entity.toHBaseEntity());
		}
		log.debug("Successfully saved BaseEntity " + entity.getCode());
	}

	/**
	 * Save a {@link Question} to the database.
	 * 
	 * @param question A {@link Question} object to save
	 */
	@Transactional
	public void saveQuestion(Question question) {

		log.info("Saving Question " + question.getCode());

		Question existingQuestion = null;
		try {
			existingQuestion = findQuestionByCode(question.getRealm(), question.getCode());
		} catch (NoResultException e) {
			log.debugf("%s not found in database, creating new row...", question.getCode());
		}

		if (existingQuestion == null) {
			entityManager.persist(question);
		} else {
			entityManager.merge(question);
		}
		log.info("Successfully saved Question " + question.getCode());
	}

	/**
	 * Save a {@link QuestionQuestion} to the database.
	 * 
	 * @param questionQuestion A {@link QuestionQuestion} object to save
	 */
	@Transactional
	public void saveQuestionQuestion(QuestionQuestion questionQuestion) {

		String sourceCode = questionQuestion.getParentCode();
		String targetCode = questionQuestion.getChildCode();
		log.info("Saving QuestionQuestion " + sourceCode + ":" + targetCode);

		QuestionQuestion existingQuestionQuestion = null;
		try {
			existingQuestionQuestion = findQuestionQuestionBySourceAndTarget(
					questionQuestion.getRealm(),
					sourceCode,
					targetCode);
		} catch (NoResultException e) {
			log.debugf("%s:%s not found in database, creating new row...",
					sourceCode, targetCode);
		}

		if (existingQuestionQuestion == null) {
			entityManager.persist(questionQuestion);
		} else {
			entityManager.merge(questionQuestion);
		}

		log.info("Successfully saved QuestionQuestion " + sourceCode + ":" + targetCode);
	}

	/**
	 * Delete a Validation from the database.
	 * 
	 * @param realm realm to delete in
	 * @param code  Code of the Validation to delete.
	 */
	@Transactional
	public void deleteValidation(String realm, String code) {

		log.info("Deleting Validation " + code);

		entityManager.createQuery("DELETE Validation WHERE realm=:realmStr AND code=:code")
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.executeUpdate();

		log.info("Successfully deleted Validation " + code + " in realm " + realm);
	}

	/**
	 * Delete an atttribute from the database.
	 * 
	 * @param realm realm to delete in
	 * @param code  Code of the attribute to delete.
	 */
	@Transactional
	public void deleteAttribute(String realm, String code) {

		log.info("Deleting Attribute " + code);

		entityManager.createQuery("DELETE Attribute WHERE realm=:realmStr AND code=:code")
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.executeUpdate();

		log.info("Successfully deleted Attribute " + code + " in realm " + realm);
	}

	/**
	 * Delete a BaseEntity from the database.
	 * 
	 * @param realm realm to delete in
	 * @param code  Code of the BaseEntity to delete.
	 */
	@Transactional
	public void deleteBaseEntity(String realm, String code) {

		log.info("Deleting BaseEntity " + code);

		entityManager.createQuery("DELETE HBaseEntity WHERE realm=:realmStr AND code=:code")
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.executeUpdate();

		log.info("Successfully deleted BaseEntity " + code + " in realm " + realm);
	}

	/**
	 * Delete a Question from the database.
	 * 
	 * @param realm realm to delete in
	 * @param code  Code of the Question to delete.
	 */
	@Transactional
	public void deleteQuestion(String realm, String code) {

		log.info("Deleting Question " + code);

		entityManager.createQuery("DELETE Question WHERE realm=:realmStr AND code=:code")
				.setParameter("realmStr", realm)
				.setParameter("code", code)
				.executeUpdate();

		log.info("Successfully deleted Question " + code + " in realm " + realm);
	}

	/**
	 * Delete a QuestionQuestion from the database.
	 * 
	 * @param realm      the realm to delete in
	 * @param sourceCode the sourceCode to delete by
	 * @param targetCode the targetCode to delete by
	 */
	@Transactional
	public void deleteQuestionQuestion(String realm, String sourceCode, String targetCode) {

		log.info("Deleting QuestionQuestion " + sourceCode + ":" + targetCode + " in realm " + realm);

		entityManager.createQuery(
				"DELETE QuestionQuestion WHERE realm=:realmStr AND sourceCode=:sourceCode AND targetCode=:targetCode")
				.setParameter("realmStr", realm)
				.setParameter("sourceCode", sourceCode)
				.setParameter("targetCode", targetCode)
				.executeUpdate();

		log.info("Successfully deleted QuestionQuestion " + sourceCode + ":" + targetCode + " in realm " + realm);
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

}
