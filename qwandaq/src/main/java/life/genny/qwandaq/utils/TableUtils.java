package life.genny.qwandaq.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.UserToken;
import org.jboss.logging.Logger;


/**
 * A utility class to assist in any Qwanda Engine Question
 * and Answer operations.
 * 
 * @author Jasper Robison
 */
@ApplicationScoped
public class TableUtils {

	static final Logger log = Logger.getLogger(TableUtils.class);

	public static Boolean searchAlt = true;

	static Integer MAX_SEARCH_HISTORY_SIZE = 10;
	static Integer MAX_SEARCH_BAR_TEXT_SIZE = 20;

	private final ExecutorService executorService = Executors.newFixedThreadPool(GennySettings.executorThreadCount());

	static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;

	@Inject
	SearchUtils searchUtils;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	UserToken userToken;

	public TableUtils() {
	}

	
	
	
	
	
	

}
