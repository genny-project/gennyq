package life.genny.fyodor.endpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.vertx.core.http.HttpServerRequest;
import life.genny.qwandaq.Ask;
import life.genny.qwandaq.ContextList;
import life.genny.qwandaq.Link;
import life.genny.qwandaq.Question;
import life.genny.qwandaq.QuestionQuestion;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.serviceq.Service;

/**
 * QwandaEndpoint --- A temporary replacement for the api service
 *
 * @author adam@gada.io
 * @author jasper.robison@gada.io
 *
 */
@Path("/qwanda")
public class QwandaEndpoint {

	private static final Logger log = Logger.getLogger(QwandaEndpoint.class);

	static Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpServerRequest request;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	Service service;

	@Inject
	UserToken userToken;

	@Inject
	ServiceToken serviceToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	EntityManager entityManager;

	@GET
	@Path("/baseentitys/{sourceCode}/asks2/{questionCode}/{targetCode}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createAsks3(
			@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, 
			@PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo
			) {

		if (userToken == null) {
			log.error("Bad or no header token in request");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		BaseEntity source = null;
		BaseEntity target = null;

		if ("PER_SOURCE".equals(sourceCode) && "PER_TARGET".equals(targetCode)) {
			source = new BaseEntity(sourceCode, "SourceCode");
			target = new BaseEntity(targetCode, "TargetCode");
		} else {
			source = beUtils.getBaseEntityByCode(sourceCode);
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		if (source == null) {
			log.error("No Source entity found!");
			return null;
		}

		if (target == null) {
			log.error("No Target entity found!");
			return null;
		}

		log.info("Fetching asks -> " + questionCode + ":" + source.getCode() + ":" + target.getCode());

		// fetch question from DB
		Ask ask = qwandaUtils.generateAskFromQuestionCode(questionCode, source, target);

		if (ask == null) {
			log.error("No ask returned for " + questionCode);
			return null;
		}

		// create ask msg from asks
		log.info("Creating ask Message...");
		QDataAskMessage msg = new QDataAskMessage(ask);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);

		String json = jsonb.toJson(msg);

		return Response.ok().entity(json).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntityEntitys(@PathParam("targetCode") String targetCode) {

		log.info("Request for EntityEntitys " + targetCode);

		if (userToken == null) {
			log.error("Bad or no header token in entityentity GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		return Response.ok().build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/parents")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntityEntitysParents(@PathParam("targetCode") String targetCode) {

		log.info("Request for EntityEntitys Parents " + targetCode);

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		if (userToken == null) {
			log.error("Bad or no header token in entityentityParents GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		List<Link> items = databaseUtils.findParentLinks(userToken.getProductCode(), targetCode);
		Link[] array = items.toArray(new Link[0]);
		String json = jsonb.toJson(array);

		return Response.ok().entity(json).build();
	}

	@POST
	@Path("/baseentitys")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveBaseEntity(BaseEntity baseEntity) {

		log.info("Request to SAVE BaseEntity");

		log.info("GENNY_TOKEN = " + userToken);
		log.info("SERVICE_TOKEN = " + serviceToken);

		if (userToken == null) {
			log.error("Bad or no header token in entityentityParents GET provided");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		beUtils.updateBaseEntity(baseEntity);

		return Response.ok().build();
	}

	/**
	 * @param rootQuestion the rootQuestion to find with
	 * @param source       the source to use
	 * @param target       the target to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> findAsks(final Question rootQuestion, final BaseEntity source, final BaseEntity target) {

		return findAsks(rootQuestion, source, target, false, false, false, false);
	}

	/**
	 * @param rootQuestion                   the rootQuestion to find with
	 * @param source                         the source to use
	 * @param target                         the target to use
	 * @param childQuestionIsMandatory       the childQuestionIsMandatory to use
	 * @param childQuestionIsReadonly        the childQuestionIsReadonly to use
	 * @param childQuestionIsFormTrigger     the childQuestionIsFormTrigger to use
	 * @param childQuestionIsCreateOnTrigger the childQuestionIsCreateOnTrigger to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> findAsks(final Question rootQuestion, final BaseEntity source, final BaseEntity target,
			Boolean childQuestionIsMandatory, Boolean childQuestionIsReadonly, Boolean childQuestionIsFormTrigger,
			Boolean childQuestionIsCreateOnTrigger) {

		String productCode = userToken.getProductCode();

		if (rootQuestion == null) {
			log.error(
					"rootQuestion for findAsks is null - source=" + source.getCode() + ": target " + target.getCode());
			return new ArrayList<Ask>();
		}

		List<Ask> asks = new ArrayList<>();
		Boolean mandatory = rootQuestion.getMandatory() || childQuestionIsMandatory;
		Boolean readonly = rootQuestion.getReadonly() || childQuestionIsReadonly;
		Ask ask = null;

		// check if this already exists?
		List<Ask> myAsks = databaseUtils.findAsksByQuestionCode(productCode, rootQuestion.getCode(),
				source.getCode(), target.getCode());
		if (!(myAsks == null || myAsks.isEmpty())) {
			ask = myAsks.get(0);
			ask.setMandatory(mandatory);
			ask.setReadonly(readonly);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setCreateOnTrigger(childQuestionIsCreateOnTrigger);

		} else {
			ask = new Ask(rootQuestion, source.getCode(), target.getCode(), mandatory, 1.0, false, false, readonly);
			ask.setCreateOnTrigger(childQuestionIsMandatory);
			ask.setFormTrigger(childQuestionIsFormTrigger);
			ask.setRealm(productCode);

			// Now merge ask name if required
			ask = performMerge(ask);
		}

		// create one
		if (rootQuestion.getAttributeCode().startsWith(Question.QUESTION_GROUP_ATTRIBUTE_CODE)) {
			// Recurse!
			List<QuestionQuestion> qqList = new ArrayList<>(rootQuestion.getChildQuestions());
			Collections.sort(qqList); // sort by priority
			List<Ask> childAsks = new ArrayList<>();
			for (QuestionQuestion qq : qqList) {
				String qCode = qq.getPk().getTargetCode();
				log.info(qq.getPk().getSourceCode() + " -> Child Question -> " + qCode);
				Question childQuestion = databaseUtils.findQuestionByCode(productCode, qCode);
				// Grab whatever icon the QuestionQuestion has set
				childQuestion.setIcon(qq.getIcon());
				if (childQuestion != null) {
					List<Ask> askChildren = null;
					try {
						askChildren = findAsks(childQuestion, source, target, qq.getMandatory(), qq.getReadonly(),
								qq.getFormTrigger(), qq.getCreateOnTrigger());
						for (Ask child : askChildren) {
							child.setQuestion(childQuestion);
							child.setHidden(qq.getHidden());
							child.setDisabled(qq.getDisabled());
							child.setReadonly(qq.getReadonly());
						}
					} catch (Exception e) {
						log.error("Error with QuestionQuestion: " + rootQuestion.getCode());
						log.error("Problem Question: " + childQuestion.getCode());
						e.printStackTrace();
					}
					childAsks.addAll(askChildren);
				}
			}
			Ask[] asksArray = childAsks.toArray(new Ask[0]);
			ask.setChildAsks(asksArray);

			ask.setRealm(productCode);
		}

		asks.add(ask);
		return asks;
	}

	/**
	 * @param rootQuestion the rootQuestion to create with
	 * @param source       the source to use
	 * @param target       the target to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> createAsksByQuestion(final Question rootQuestion, final BaseEntity source,
			final BaseEntity target) {

		List<Ask> asks = findAsks(rootQuestion, source, target);

		return asks;
	}

	/**
	 * @param questionCode the questionCode to use
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @return List&lt;Ask&gt;
	 */
	public List<Ask> createAsksByQuestionCode(final String questionCode, final String sourceCode,
			final String targetCode) {

		Question rootQuestion = databaseUtils.findQuestionByCode(userToken.getProductCode(), questionCode);
		BaseEntity source = null;
		BaseEntity target = null;

		if ("PER_SOURCE".equals(sourceCode) && "PER_TARGET".equals(targetCode)) {
			source = new BaseEntity(sourceCode, "SourceCode");
			target = new BaseEntity(targetCode, "TargetCode");
		} else {
			source = beUtils.getBaseEntityByCode(sourceCode);
			target = beUtils.getBaseEntityByCode(targetCode);
		}

		return createAsksByQuestion(rootQuestion, source, target);
	}

	/**
	 * @param sourceCode   the sourceCode to use
	 * @param targetCode   the targetCode to use
	 * @param questionCode the questionCode to use
	 * @return QDataAskMessage
	 */
	public QDataAskMessage getDirectAsks(String sourceCode, String targetCode, String questionCode) {
		List<Ask> asks = null;

		asks = createAsksByQuestionCode(questionCode, sourceCode, targetCode);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		return askMsgs;
	}

	/**
	* Get all attribute codes active within an ask using recursion.
	*
	* @param codes The set of codes to add to
	* @param ask The ask to traverse
	* @return The udpated set of codes
	 */
	public Set<String> recursivelyGetAttributeCodes(Set<String> codes, Ask ask) {

		// grab attribute code of current ask
		codes.add(ask.getAttributeCode());

		if ((ask.getChildAsks() != null) && (ask.getChildAsks().length > 0)) {

			// grab all child ask attribute codes
			for (Ask childAsk : ask.getChildAsks()) {

				codes.addAll(recursivelyGetAttributeCodes(codes, childAsk));
			}
		}
		return codes;
	}
	
	/**
	 * Perform a merge of ask data.
	 *
	 * @param ask the ask to merge
	 * @return Ask
	 */
	private Ask performMerge(Ask ask) {

		if (ask.getName().contains("{{")) {
			// now merge in data
			String name = ask.getName();

			Map<String, Object> templateEntityMap = new HashMap<>();
			ContextList contexts = ask.getContextList();
			for (life.genny.qwandaq.Context context : contexts.getContexts()) {
				BaseEntity be = context.getEntity();
				templateEntityMap.put(context.getName(), be);
			}
			String mergedName = MergeUtils.merge(name, templateEntityMap);
			ask.setName(mergedName);
		}
		return ask;

	}
}
