package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.exception.BadDataException;
import life.genny.qwandaq.message.QDataBaseEntityMessage;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KafkaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class SearchService {

	private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	public void sendSearch(String eventCode) {

		String searchCode = "SBE_"+StringUtils.removeStart(eventCode, "QUE_");
		searchUtils.searchTable(searchCode);

		BaseEntity content = beUtils.getBaseEntityByCode("PCM_CONTENT");
		try {
            content.setValue("PRI_LOC1", "PCM_TABLE");
        } catch (BadDataException e) {
            e.printStackTrace();
        }

		BaseEntity table = beUtils.getBaseEntityByCode("PCM_TABLE");
		try {
            table.setValue("PRI_LOC1", searchCode);
        } catch (BadDataException e) {
            e.printStackTrace();
        }

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(content);
		msg.add(table);
		msg.setToken(userToken.getToken());
		msg.setReplace(true);
		KafkaUtils.writeMsg("webcmds", msg);
	}

	public void sendBuckets() {
	}

}
