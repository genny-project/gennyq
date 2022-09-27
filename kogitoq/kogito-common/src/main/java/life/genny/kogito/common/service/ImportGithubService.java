package life.genny.kogito.common.service;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;

@ApplicationScoped
public class ImportGithubService {

	static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());

	static Jsonb jsonb = JsonbBuilder.create();

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
	 * Import github files
	 *
	 * @return true if successful
	 */
	public Boolean importGithubFiles() {
		log.info("=========================Import Github Files=========================");
		log.info("Source Code = " + serviceToken.getUserCode());

		return true;
	}

}