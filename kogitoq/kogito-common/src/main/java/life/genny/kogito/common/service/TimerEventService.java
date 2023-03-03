package life.genny.kogito.common.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.SearchUtils;
import org.jboss.logging.Logger;

import life.genny.kogito.common.models.TimerData;
import life.genny.kogito.common.models.TimerEvent;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.search.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;

@ApplicationScoped
public class TimerEventService extends KogitoService {

	@Inject
	Logger log;

	@Inject
	ServiceToken serviceToken;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	SearchUtils searchUtils;

	/**
	 * Fetch the TimerData for a questionCode.
	 * 
	 * @param questionCode
	 */
	public TimerData fetchTimerData(final String productCode, String questionCode, String pcmCode) {

		if (questionCode == null)
			return new TimerData();

		log.debug("Fetching TimerEvents " + productCode + " and " + questionCode);

		// TODO, fetch the TimerEvent codes from the questionCode BE

		SearchEntity searchEntity = new SearchEntity("SBE_TIMEREVENTS", "TimerEvents")
				.add(new Filter(Attribute.PRI_CODE, Operator.LIKE, "TEV_%"))
				.add(new Filter("LNK_QUESTION", Operator.LIKE, "%\"" + questionCode.toUpperCase() + "\"%"))
				.add(new Column("PRI_MILESTONE", "Milestone"))
				.add(new Column("PRI_ATTRIBUTECODE_VALUES", "Settings"))
				.add(new Column("PRI_MINUTES", "Minutes"))
				.setPageStart(0)
				.setPageSize(100);

		searchEntity.setRealm(productCode);

		List<BaseEntity> timerEventBEs = searchUtils.searchBaseEntitys(searchEntity);

		TimerData timerData = new TimerData();

		timerData.setElapsedMin(0L);
		if (timerEventBEs != null) {
			for (BaseEntity timerEventBE : timerEventBEs) {

				String timerEventBECode = timerEventBE.getCode();
				log.info("Processing TimerEvent " + timerEventBECode);
				EntityAttribute milestoneAttribute = beaUtils.getEntityAttribute(productCode, timerEventBE.getCode(), "PRI_MILESTONE");
				String milestoneValue = milestoneAttribute != null ? milestoneAttribute.getValueString() : null;
				log.info(" PRI_MILESTONE : " + milestoneValue);
				EntityAttribute attributeCodeValues = beaUtils.getEntityAttribute(productCode, timerEventBE.getCode(), "PRI_ATTRIBUTECODE_VALUES");
				String updatePairs = attributeCodeValues != null ? attributeCodeValues.getValueString() : null;
				log.info(" PRI_ATTRIBUTECODE_VALUES : " + attributeCodeValues);
				EntityAttribute priMinutes = beaUtils.getEntityAttribute(productCode, timerEventBE.getCode(), "PRI_MINUTES");
				Integer priMinutesVal = priMinutes != null ? priMinutes.getValueInteger() : 0;
				log.info(" PRI_MINUTES : " + priMinutesVal);
				TimerEvent timerEvent = new TimerEvent();
				timerEvent.setTimeStamp((long) priMinutesVal);
				timerEvent.setUniqueCode(milestoneValue);
				timerEvent.setUpdatePairs(updatePairs);
				timerData.add(timerEvent);
			}
		}

		return timerData;
	}

}
