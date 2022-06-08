package life.genny.gadaq.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.EEntityStatus;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.message.QDataAskMessage;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class BaseEntityService {

	private static final Logger log = Logger.getLogger(BaseEntityService.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	BaseEntityUtils beUtils;

	public void commission() {

		String defCode = null;

		if (defCode == null || !defCode.startsWith("DEF_")) {
			log.error("Invalid defCode: " + defCode);
			return;
		}

		// fetch the def baseentity
		BaseEntity def = beUtils.getBaseEntityByCode(defCode);
		if (def == null) {
			log.error("Could not find DEF BaseEntity with code: " + defCode);
		}

		// use entity create function and save to db
		try {
			BaseEntity entity = beUtils.create(def);
			log.info("BaseEntity Created: " + entity.getCode());
		} catch (Exception e) {
			log.error("Error creating BaseEntity! DEF Code: " + defCode);
			e.printStackTrace();
		}
	}

	public void decommission() {

		BaseEntity baseEntity = null;

		if (baseEntity == null) {
			log.error("BaseEntity passed is null!");
			return;
		}

		log.info("Decommissioning entity " + baseEntity.getCode());

		// archive the entity
		baseEntity.setStatus(EEntityStatus.ARCHIVED);
		beUtils.updateBaseEntity(baseEntity);
	}
}
