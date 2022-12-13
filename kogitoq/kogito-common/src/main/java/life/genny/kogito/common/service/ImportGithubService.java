package life.genny.kogito.common.service;

import life.genny.qwandaq.Answer;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ServiceToken;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jboss.logging.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.GithubUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import life.genny.qwandaq.utils.SearchUtils;
import life.genny.qwandaq.utils.DefUtils;

@ApplicationScoped
public class ImportGithubService {

	@Inject
	static Logger log;

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

	@Inject
	DefUtils defUtils;

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
			bes = githubUtils.getLayoutBaseEntitys(beUtils, gitUrl, branch, realm, gitrealm, recursive);
		} catch (RevisionSyntaxException | GitAPIException | IOException e) {
			e.printStackTrace();
		}
		log.info("bes = " + bes.size());
		Definition dotDef = Definition.from(beUtils.getBaseEntity(realm, "DEF_DOCUMENT_TEMPLATE"));
		for (BaseEntity be : bes) {
			if (be != null) {
				log.info("saving be = " + be.getCode() + ":" + be.getName());
				BaseEntity newBe = beUtils.create(dotDef, be.getName(), be.getCode());
				newBe.setRealm(be.getRealm());

				newBe.addAnswer(new Answer(newBe, newBe, qwandaUtils.getAttribute("PRI_NAME"), be.getName()));
				newBe.addAnswer(new Answer(newBe, newBe, qwandaUtils.getAttribute("PRI_CODE"), be.getCode()));
				newBe.addAnswer(new Answer(newBe, newBe, qwandaUtils.getAttribute("PRI_HTML_MERGE"),
						be.getValueAsString("PRI_HTML_MERGE")));

				beUtils.updateBaseEntity(newBe);
			} else {
				log.error("Fetched null baseentity");
			}
		}
		return true;
	}

}
