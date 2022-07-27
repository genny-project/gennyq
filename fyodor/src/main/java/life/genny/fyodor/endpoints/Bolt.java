package life.genny.fyodor.endponts;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.SecurityUtils;
import life.genny.qwandaq.validation.Validation;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.serviceq.Service;

@Path("/bolt")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Bolt {

	static final Logger log = Logger.getLogger(Bolt.class);
	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

	@Inject 
	DatabaseUtils dbUtils;

	@Inject
	BaseEntityUtils beUtils;

	/**
	* Get all Validations from the database in pages.
	* If pageSize and pageNum are null, all items will be requested at once.
	*
	* @param token
	* @param productCode
	* @param pageSize
	* @param pageNum
	* @return An Http response containing a json list of Validations.
	 */
	@GET
	@Path("/validations/{productCode}")
	public Response validations(
			@HeaderParam("Authorization") String token, 
			@PathParam("productCode") String productCode, 
			@QueryParam("pageSize") Integer pageSize, 
			@QueryParam("pageNum") Integer pageNum,
			@QueryParam("wildcard") String wildcard) {

		log.info("Fetching validations...");

		List<Validation> validations = dbUtils.findValidations(productCode, pageSize, pageNum, wildcard);
		String json = jsonb.toJson(validations);

		return Response.ok().entity(json).build();
	}

	/**
	* Get all Attributes from the database in pages.
	* If pageSize and pageNum are null, all items will be requested at once.
	*
	* @param token
	* @param productCode
	* @param pageSize
	* @param pageNum
	* @return An Http response containing a json list of Attributes.
	 */
	@GET
	@Path("/attributes/{productCode}")
	public Response attributes(
			@HeaderParam("Authorization") String token, 
			@PathParam("productCode") String productCode, 
			@QueryParam("pageSize") Integer pageSize, 
			@QueryParam("pageNum") Integer pageNum,
			@QueryParam("wildcard") String wildcard) {

		log.info("Fetching attributes...");

		List<Attribute> attributes = dbUtils.findAttributes(productCode, pageSize, pageNum, wildcard);
		String json = jsonb.toJson(attributes);

		return Response.ok().entity(json).build();
	}

	/**
	* Get all BaseEntitys from the database in pages.
	* If pageSize and pageNum are null, all items will be requested at once.
	*
	* @param token
	* @param productCode
	* @param pageSize
	* @param pageNum
	* @return An Http response containing a json list of BaseEntitys.
	 */
	@GET
	@Path("/baseentitys/{productCode}")
	public Response baseentitys(
			@HeaderParam("Authorization") String token, 
			@PathParam("productCode") String productCode, 
			@QueryParam("pageSize") Integer pageSize, 
			@QueryParam("pageNum") Integer pageNum,
			@QueryParam("wildcard") String wildcard) {

		log.info("Fetching baseentitys...");

		List<BaseEntity> entities = dbUtils.findBaseEntitys(productCode, pageSize, pageNum, wildcard);
		String json = jsonb.toJson(entities);

		return Response.ok().entity(json).build();
	}

	/**
	* Get all Questions from the database in pages.
	* If pageSize and pageNum are null, all items will be requested at once.
	*
	* @param token
	* @param productCode
	* @param pageSize
	* @param pageNum
	* @return An Http response containing a json list of Questions.
	 */
	@GET
	@Path("/questions/{productCode}")
	public Response questions(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNum") Integer pageNum,
			@QueryParam("wildcard") String wildcard) {

		log.info("Fetching questions...");

		List<Question> questions = dbUtils.findQuestions(productCode, pageSize, pageNum, wildcard);
		String json = jsonb.toJson(questions);

		return Response.ok().entity(json).build();
	}

	/**
	* Get all QuestionQuestions from the database in pages.
	* If pageSize and pageNum are null, all items will be requested at once.
	*
	* @param token
	* @param productCode
	* @param pageSize
	* @param pageNum
	* @return An Http response containing a json list of QuestionQuestions.
	 */
	@GET
	@Path("/questionquestions/{productCode}")
	public Response questionQuestions(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNum") Integer pageNum,
			@QueryParam("wildcard") String wildcard) {

		log.info("Fetching question_questions...");

		List<QuestionQuestion> questionQuestions = dbUtils.findQuestionQuestions(productCode, pageSize, pageNum, wildcard);
		String json = jsonb.toJson(questionQuestions);

		return Response.ok().entity(json).build();
	}

	@PUT
	@Path("/validation/{productCode}")
	public Response validations(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, Validation validation) {

		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		dbUtils.saveValidation(validation);

		return Response.ok().entity(HttpUtils.ok()).build();
	}

	@PUT
	@Path("/attribute/{productCode}")
	public Response attributes(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, Attribute attribute) {

		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		dbUtils.saveAttribute(attribute);

		return Response.ok().entity(HttpUtils.ok()).build();
	}

	@PUT
	@Path("/question/{productCode}")
	public Response questions(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, Question question) {

		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		dbUtils.saveQuestion(question);

		return Response.ok().entity(HttpUtils.ok()).build();
	}

	@PUT
	@Path("/questionquestion/{productCode}")
	public Response questionQuestions(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, QuestionQuestion questionQuestion) {

		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		dbUtils.saveQuestionQuestion(questionQuestion);

		return Response.ok().entity(HttpUtils.ok()).build();
	}

	@POST
	@Path("/answer/{productCode}")
	public Response baseentitys(@HeaderParam("Authorization") String token, @PathParam("productCode") String productCode, Answer answer) {

		Boolean authorized = SecurityUtils.isAuthorizedToken(token);
		if (!authorized) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Not authorized to make this request")).build();
		}

		log.info("Saving baseentity...");

		String sourceCode = answer.getSourceCode();
		String targetCode = answer.getTargetCode();
		String attributeCode = answer.getAttributeCode();
		String value = answer.getValue();
		Double weight = answer.getWeight();

		// null check any parameters
		if (sourceCode == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("No Source Code Specified")).build();
		}

		if (targetCode == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("No Target Code Specified")).build();
		}

		if (attributeCode == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("No Attribute Code Specified")).build();
		}

		if (value == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("No Value Specified")).build();
		}

		if (weight == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("No Weight Specified")).build();
		}

		BaseEntity source = dbUtils.findBaseEntityByCode(productCode, sourceCode);
		BaseEntity target = dbUtils.findBaseEntityByCode(productCode, targetCode);
		Attribute attribute = dbUtils.findAttributeByCode(productCode, attributeCode);

		if (target == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Target doesn't exist")).build();
		}

		if (attribute == null) {
			return Response.status(Response.Status.BAD_REQUEST)
				.entity(HttpUtils.error("Attribute doesn't exist")).build();
		}

		// handle special cases
		if (attributeCode.equals("PRI_CODE")) {
			target.setCode((String) value);
		} else if (attributeCode.equals("PRI_NAME")) {
			target.setName((String) value);
		}

		answer = new Answer(source, target, attribute, value);
		answer.setWeight(weight);

		try {
			target.addAnswer(answer);
		} catch (BadDataException e) {
			log.errorf("Could not add answer for %s:%s:%s", targetCode, attributeCode, value);
		}

		// save entity and send back in response
		dbUtils.saveBaseEntity(target);

		// update from DB again before sending back
		target = dbUtils.findBaseEntityByCode(productCode, targetCode);
		String json = jsonb.toJson(target);

		return Response.ok().entity(json).build();
	}

}
