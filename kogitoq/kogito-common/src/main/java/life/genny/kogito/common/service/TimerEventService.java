package life.genny.kogito.common.service;

import life.genny.kogito.common.models.TimerData;
import life.genny.kogito.common.models.TimerEvent;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.entity.SearchEntity;
import life.genny.qwandaq.entity.search.trait.Column;
import life.genny.qwandaq.entity.search.trait.Filter;
import life.genny.qwandaq.entity.search.trait.Operator;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.SearchUtils;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.invoke.MethodHandles;
import java.util.List;

@ApplicationScoped
public class TimerEventService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	ServiceToken serviceToken;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	SearchUtils searchUtils;

	/**
	 * Fetch the TimerData for a questionCode.
	 * 
	 * @param questionCode
	 */
	public TimerData fetchTimerData(final String productCode, final String questionCode) {

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

				log.info("Processing TimerEvent " + timerEventBE.getCode());
				log.info(" PRI_MILESTONE : " + timerEventBE.getValue("PRI_MILESTONE"));
				log.info(" PRI_ATTRIBUTECODE_VALUES : " + timerEventBE.getValue("PRI_ATTRIBUTECODE_VALUES"));
				log.info(" PRI_MINUTES : " + timerEventBE.getValue("PRI_MINUTES", 0));
				TimerEvent timerEvent = new TimerEvent();
				timerEvent.setTimeStamp((long) timerEventBE.getValue("PRI_MINUTES", 0));
				timerEvent.setUniqueCode(timerEventBE.getValueAsString("PRI_MILESTONE"));
				timerEvent.setUpdatePairs(timerEventBE.getValueAsString("PRI_ATTRIBUTECODE_VALUES"));
				timerData.add(timerEvent);
			}
		}

		return timerData;
	}

}
