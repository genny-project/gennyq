package life.genny.kogito.common.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.GithubUtils;
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

		GithubUtils githubUtils = new GithubUtils();
		String gitUrl = "https://github.com/genny-project/layouts.git";
		String branch = "master";
		String realm = "internmatch";
		String gitrealm = "internmatch-new/document_templates";
		Boolean recursive = true;

		log.info(gitUrl + ":" + branch + ":" + realm + ":" + gitrealm + ":" + recursive);

		List<BaseEntity> bes = new ArrayList<>();
		try {
			bes = githubUtils.getLayoutBaseEntitys(gitUrl, branch, realm, gitrealm, recursive);
		} catch (RevisionSyntaxException | GitAPIException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("bes = " + bes.size());
		for (BaseEntity be : bes) {
			log.info("be = " + be.getCode() + ":" + be.getName());
		}
		return true;
	}

}