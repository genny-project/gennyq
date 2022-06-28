package life.genny.qwandaq.utils;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;

import io.smallrye.common.annotation.NonBlocking;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.QuestionQuestionId;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.validation.Validation;

/*
 * A  utility class used for standard read and write 
 * operations to the database.
 * 
 * @author Jasper Robison
 * @author Bryn Mecheam
 */
@ApplicationScoped
public class DatabaseUtils {

	static final Logger log = Logger.getLogger(DatabaseUtils.class);
	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	EntityManager entityManager;

	/**
	 * Check if entityManager is present.
	 * 
	 * @return whether or not entityManager is present
	 */
	public boolean checkEntityManager() {

		if (entityManager == null) {
			log.error("EntityManager must be initialised first!!!");
			return false;
		}

		return true;
	}

	/**
	 * Get all attributes with a specific Prefix
	 * @param productCode - Product Code to load from
	 * @param prefix - Prefix to check for
	 * @return A list of attributes
	 */
	// TODO: Lint this against accepted prefixes
	public List<Attribute> findAttributesWithPrefix(String productCode, String prefix) {
		return findAttributes(productCode, null, null, "PRM_");		
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
	@Transactional
	public List<Validation> findValidations(String realm, Integer pageSize, Integer pageNumber,
			String wildcard) {

		checkEntityManager();

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());

		String queryStr = "FROM Validation WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");

		try {
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

		} catch (NoResultException e) {
			log.debug("No attributes found from DB search");
		}

		return null;
	}

	@Transactional
	public Long countAttributes(String realm) {

		checkEntityManager();

		try {
			Query query = entityManager
					.createQuery("SELECT count(1) FROM Attribute WHERE realm=:realmStr AND name not like 'App\\_%'")
					.setParameter("realmStr", realm);

			return (Long) query.getResultList().get(0);
		} catch (NoResultException e) {
			log.debug("No Attributes found from DB Search");
		}

		return 0L;
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
	// @Transactional
	public List<Attribute> findAttributes(String realm, Integer startIdx, Integer pageSize, String wildcard) {

		checkEntityManager();

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());

		String queryStr = "FROM Attribute WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "")
				+ " AND name not like 'App\\_%' order by id";

		try {
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

		} catch (NoResultException e) {
			log.debug("No attributes found from DB search");
		}

		return null;
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
	@Transactional
	public List<BaseEntity> findBaseEntitys(String realm, Integer pageSize, Integer pageNumber,
			String wildcard) {

		checkEntityManager();

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());

		String queryStr = "FROM BaseEntity WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");

		try {

			Query query = entityManager.createQuery(queryStr, BaseEntity.class)
					.setParameter("realmStr", realm);

			if (isWildcard) {
				query.setParameter("code", "%" + wildcard + "%");
			}

			if (pageNumber != null && pageSize != null) {
				query = query.setFirstResult((pageNumber - 1) * pageSize)
						.setMaxResults(pageSize);
			}

			return query.getResultList();

		} catch (NoResultException e) {
			log.debug("No BaseEntitys found in DB for realm " + realm);
		}

		return null;
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
	@Transactional
	public List<Question> findQuestions(String realm, Integer pageSize, Integer pageNumber, String wildcard) {

		checkEntityManager();

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());

		String queryStr = "FROM Question WHERE realm=:realmStr" + (isWildcard ? " AND code like :code" : "");

		try {

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

		} catch (NoResultException e) {
			log.debug("No Question found in DB for realm " + realm);
		}

		return null;
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
	@Transactional
	public List<QuestionQuestion> findQuestionQuestions(String realm, Integer pageSize, Integer pageNumber,
			String wildcard) {

		checkEntityManager();

		Boolean isWildcard = (wildcard != null && !wildcard.isEmpty());

		String queryStr = "FROM QuestionQuestion WHERE realm=:realmStr"
				+ (isWildcard ? " AND sourceCode like :code" : "");

		try {

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

		} catch (NoResultException e) {
			log.debug("No QuestionQuestion found in DB for realm " + realm);
		}

		return null;
	}

	/**
	 * Grab a Validation from the database using a code and a realm.
	 *
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Validation
	 */

	public Validation findValidationByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM Validation WHERE realm=:realmStr AND code=:code",
							Validation.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.debug("No Validation found in DB for " + code + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Fetch an Attribute from the database using a realm and a code.
	 *
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Attribute
	 */

	public Attribute findAttributeByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM Attribute WHERE realm=:realmStr AND code =:code", Attribute.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.debug("No Attribute found in DB for " + code + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Fetch A {@link BaseEntity} from the database using the entity code.
	 *
	 * @param realm The realm that the {@link BaseEntity} is saved under
	 * @param code  The code of the {@link BaseEntity} to fetch
	 * @return The corresponding BaseEntity, or null if not found.
	 */
	public BaseEntity findBaseEntityByCode(String realm, String code) {

		checkEntityManager();
		try {

			return entityManager
					.createQuery("FROM BaseEntity WHERE realm=:realmStr AND code=:code", BaseEntity.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.info("No BaseEntity found in DB for " + code + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Fetch A {@link Question} from the database using the question code.
	 *
	 * @param realm the realm to find in
	 * @param code  the code to find by
	 * @return Question
	 */

	public Question findQuestionByCode(String realm, String code) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery("FROM Question WHERE realm=:realmStr AND code=:code", Question.class)
					.setParameter("realmStr", realm)
					.setParameter("code", code)
					.getSingleResult();

		} catch (NoResultException e) {
			log.debug("No Question found in DB for " + code + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Find a QuestionQuestion using a realm, a sourceCode and a targetCode.
	 *
	 * @param realm      the realm to find in
	 * @param sourceCode the sourceCode to find by
	 * @param targetCode the targetCode to find by
	 * @return List list of QuestionQuestions
	 */

	public QuestionQuestion findQuestionQuestionBySourceAndTarget(String realm, String sourceCode,
			String targetCode) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery(
							"FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode AND targetCode = :targetCode",
							QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.setParameter("targetCode", targetCode)
					.getSingleResult();

		} catch (NoResultException e) {
			log.debug("No QuestionQuestion found in DB for " + sourceCode + ":" + targetCode + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Find a list of QuestionQuestions using a realm and a sourceCode
	 *
	 * @param realm      the realm to find in
	 * @param sourceCode the sourceCode to find by
	 * @return List list of QuestionQuestions
	 */

	public List<QuestionQuestion> findQuestionQuestionsBySourceCode(String realm, String sourceCode) {

		checkEntityManager();

		try {

			return entityManager
					.createQuery(
							"FROM QuestionQuestion WHERE realm=:realmStr AND sourceCode = :sourceCode order by weight ASC",
							QuestionQuestion.class)
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.getResultList();

		} catch (NoResultException e) {
			log.debug("No QuestionQuestion found in DB for " + sourceCode);
		}

		return null;
	}

	/**
	 * Find a list of Asks using question code, sourceCode and targetCode.
	 *
	 * @param realm        the realm to find in
	 * @param questionCode the questionCode to find
	 * @param sourceCode   the sourceCode to find by
	 * @param targetCode   the targetCode to find by
	 * @return List list of asks
	 */

	public List<Ask> findAsksByQuestionCode(String realm, String questionCode, String sourceCode,
			String targetCode) {

		checkEntityManager();

		try {
			return entityManager
					.createQuery("FROM Ask WHERE realm=:realmStr AND sourceCode=:sourceCode"
							+ " AND targetCode=:targetCode AND questionCode=:questionCode", Ask.class)
					.setParameter("questionCode", questionCode)
					.setParameter("sourceCode", sourceCode)
					.setParameter("realmStr", realm)
					.setParameter("targetCode", targetCode)
					.getResultList();

		} catch (NoResultException e) {
			log.debug("No Asks found in DB for " + questionCode + ":" + sourceCode + ":" + targetCode + " in realm "
					+ realm);
		}

		return null;
	}

	public List<Link> findParentLinks(String realm, String targetCode) {

		checkEntityManager();

		try {
			return entityManager.createQuery("SELECT ee.link FROM EntityEntity ee"
					+ " where ee.pk.targetCode=:targetCode and ee.pk.source.realm=:realmStr", Link.class)
				.setParameter("targetCode", targetCode)
				.setParameter("realmStr", realm)
				.getResultList();

		} catch (NoResultException e) {
			log.debug("No Links found in DB for " + targetCode + " in realm " + realm);
		}

		return null;
	}

	/**
	 * Save a {@link Validation} to the database.
	 *
	 * @param validation A {@link Validation} object to save
	 */
	@Transactional
	public void saveValidation(Validation validation) {

		log.info("Saving Validation " + validation.getCode());

		checkEntityManager();

		Validation existingValidation = findValidationByCode(validation.getRealm(), validation.getCode());

		try {

			if (existingValidation == null) {
				entityManager.persist(validation);
			} else {
				entityManager.merge(validation);
			}

			log.info("Successfully saved Validation " + validation.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Save an {@link Attribute} to the database.
	 *
	 * @param attribute An {@link Attribute} object to save
	 * @return was the save successful
	 */
	@Transactional
	public boolean saveAttribute(Attribute attribute) {

		if (attribute == null) {
			log.error("Attempting to save null attribute!");
			return false;
		}
		log.info("Saving Attribute " + attribute.getCode());

		checkEntityManager();

		Attribute existingAttribute = findAttributeByCode(attribute.getRealm(), attribute.getCode());

		try {

			if (existingAttribute == null) {
				entityManager.persist(attribute);
			} else {
				entityManager.merge(attribute);
			}
			
			log.info("Successfully saved Attribute " + attribute.getCode());
			return true;
		} catch (Exception e) {
			log.error("Error saving attribute!: " + attribute.getCode());
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Save a {@link BaseEntity} to the database.
	 *
	 * @param entity A {@link BaseEntity} object to save
	 */
	@Transactional
	public void saveBaseEntity(BaseEntity entity) {

		log.info("Saving BaseEntity " + entity.getCode());

		checkEntityManager();

		BaseEntity existingEntity = findBaseEntityByCode(entity.getRealm(), entity.getCode());

		try {

			if (existingEntity == null) {
				entityManager.persist(entity);
			} else {
				entityManager.merge(entity);
			}

			log.info("Successfully saved BaseEntity " + entity.getCode());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Save a {@link Question} to the database.
	 *
	 * @param question A {@link Question} object to save
	 */
	@Transactional
	public void saveQuestion(Question question) {

		log.info("Saving Question " + question.getCode());

		checkEntityManager();

		Question existingQuestion = findQuestionByCode(question.getRealm(), question.getCode());

		try {

			if (existingQuestion == null) {
				entityManager.persist(question);
			} else {
				entityManager.merge(question);
			}

			log.info("Successfully saved Question " + question.getCode());

		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Save a {@link QuestionQuestion} to the database.
	 *
	 * @param questionQuestion A {@link QuestionQuestion} object to save
	 */
	@Transactional
	public void saveQuestionQuestion(QuestionQuestion questionQuestion) {

		QuestionQuestionId pk = questionQuestion.getPk();

		log.info("Saving QuestionQuestion " + pk.getSourceCode() + ":" + pk.getTargetCode());

		checkEntityManager();

		QuestionQuestion existingQuestionQuestion = findQuestionQuestionBySourceAndTarget(
				questionQuestion.getRealm(),
				pk.getSourceCode(),
				pk.getTargetCode());

		try {

			if (existingQuestionQuestion == null) {
				entityManager.persist(questionQuestion);
			} else {
				entityManager.merge(questionQuestion);
			}

			log.info("Successfully saved QuestionQuestion " + pk.getSourceCode() + ":" + pk.getTargetCode());

		} catch (Exception e) {
			log.error(e);
		}
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

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Validation WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.info("Successfully deleted Validation " + code + " in realm " + realm);

		} catch (Exception e) {
			log.error(e);
		}
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

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Attribute WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.info("Successfully deleted Attribute " + code + " in realm " + realm);

		} catch (Exception e) {
			log.error(e);
		}
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

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE BaseEntity WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.info("Successfully deleted BaseEntity " + code + " in realm " + realm);

		} catch (Exception e) {
			log.error(e);
		}
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

		checkEntityManager();

		try {
			Query q = entityManager.createQuery("DELETE Question WHERE realm=:realmStr AND code=:code")
					.setParameter("realmStr", realm)
					.setParameter("code", code);

			q.executeUpdate();
			log.info("Successfully deleted Question " + code + " in realm " + realm);

		} catch (Exception e) {
			log.error(e);
		}
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

		checkEntityManager();

		try {
			Query q = entityManager.createQuery(
					"DELETE QuestionQuestion WHERE realm=:realmStr AND sourceCode=:sourceCode AND targetCode=:targetCode")
					.setParameter("realmStr", realm)
					.setParameter("sourceCode", sourceCode)
					.setParameter("targetCode", targetCode);

			q.executeUpdate();
			log.info("Successfully deleted QuestionQuestion " + sourceCode + ":" + targetCode + " in realm " + realm);

		} catch (Exception e) {
			log.error(e);
		}
	}

}
