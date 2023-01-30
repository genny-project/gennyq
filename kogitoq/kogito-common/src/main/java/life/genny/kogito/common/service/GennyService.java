package life.genny.kogito.common.service;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;

@ApplicationScoped
public class GennyService {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

	@ConfigProperty(name = "quarkus.application.name")
	String productCode;

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	ServiceToken serviceToken;

	@Inject
	DatabaseUtils databaseUtils;

	/**
	 * setup the BaseEntity with initial data.
	 *
	 * @param code       The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean setup(String code) {
		log.info("=========================initialise BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntityCode = " + code);

		CommonUtils.getArcInstance(BaseEntityUtils.class);

		BaseEntity be = beUtils.getBaseEntity(code);

		if (be == null) {
			log.error("BaseEntity not found");
			return false;
		}

		// set core attributes
		be = beUtils.addValue(be, "PRI_CODE", code);

		beUtils.updateBaseEntity(be);

		return true;
	}

	/**
	 * setup the BaseEntity with active data.
	 *
	 * @param code The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean activate(String code) {
		log.info("=========================activate BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntity Code = " + code);

		BaseEntity be = beUtils.getBaseEntity(code);

		if (be == null) {
			log.error("BaseEntity not found");
			return false;
		}

		be = beUtils.addValue(be, "PRI_STATUS", "ACTIVE");
		be = beUtils.addValue(be, "PRI_STATUS_COLOR", "#5CB85C");

		be.setStatus(EEntityStatus.ACTIVE);

		beUtils.updateBaseEntity(be);

		return true;
	}

	/**
	 * Archive the BaseEntity with active data.
	 *
	 * @param code The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean archive(String code) {
		log.info("=========================Archive BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntity Code = " + code);

		beUtils = new BaseEntityUtils(serviceToken); // assume userToken dead.
		BaseEntity be = beUtils.getBaseEntity(code);

		if (be == null) {
			log.error("BaseEntity not found");
			return false;
		}

		be = beUtils.addValue(be, "PRI_STATUS", "ARCHIVED");
		be.setStatus(EEntityStatus.ARCHIVED);
		beUtils.updateBaseEntity(be);

		return true;
	}

	/**
	 * Cancel the BaseEntity with active data.
	 *
	 * @param code The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean cancel(String code) {
		log.info("=========================Cancel BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntity Code = " + code);

		beUtils = new BaseEntityUtils(serviceToken); // assume userToken dead.
		BaseEntity be = beUtils.getBaseEntity(code);

		if (be == null) {
			log.error("BaseEntity not found");
			return false;
		}
		be.setStatus(EEntityStatus.DELETED);
		beUtils.updateBaseEntity(be);

		return true;
	}

	/**
	 * Expiredthe BaseEntity with active data.
	 *
	 * @param code The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean expired(String code) {
		log.info("=========================Expired BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntity Code = " + code);

		beUtils = new BaseEntityUtils(serviceToken); // assume userToken dead.
		BaseEntity baseEntity = beUtils.getBaseEntity(code);

		if (baseEntity == null) {
			log.error("BaseEntity not found");
			return false;
		}
		baseEntity.setStatus(EEntityStatus.DELETED);
		beUtils.updateBaseEntity(baseEntity);

		return true;
	}

	/**
	 * Abort the BaseEntity with active data.
	 *
	 * @param code The targeted BaseEntity
	 * @return true if successful
	 */
	public Boolean abort(String code) {
		log.info("=========================Abort BaseEntity=========================");
		log.info("Source Code = " + userToken.getUserCode());
		log.info("BaseEntity Code = " + code);
		beUtils = new BaseEntityUtils(serviceToken); // assume userToken dead.

		// activate request scope and fetch UserToken
		// Arc.container().requestContext().activate();
		log.info("database lookup is for " + productCode + " and code " + code);
		BaseEntity baseEntity = databaseUtils.findBaseEntityByCode(productCode, code);

		if (baseEntity == null) {
			log.error("BaseEntity not found");
			return false;
		}
		baseEntity.setStatus(EEntityStatus.DELETED);
		databaseUtils.saveBaseEntity(baseEntity);

		return true;
	}
}
