package life.genny.kogito.common.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.utils.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.Definition;
import life.genny.qwandaq.Answer;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.ServiceToken;

@ApplicationScoped
public class ImportGithubService extends KogitoService {

	@Inject
	CacheManager cm;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	ServiceToken serviceToken;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	DefUtils defUtils;
	Logger log;

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
		Definition dotDef = Definition.from(beUtils.getBaseEntity(realm, "DEF_DOCUMENT_TEMPLATE", true));
		dotDef.setBaseEntityAttributes(beaUtils.getAllEntityAttributesForBaseEntity(realm, "DEF_DOCUMENT_TEMPLATE"));
		for (BaseEntity be : bes) {
			if (be != null) {
				String beCode = be.getCode();
				log.info("saving be = " + beCode + ":" + be.getName());
				BaseEntity newBe = beUtils.create(dotDef, be.getName(), beCode);
				String productCode = be.getRealm();
				newBe.setRealm(productCode);

				newBe.addAnswer(new Answer(newBe, newBe, cm.getAttribute("PRI_NAME"), be.getName()));
				newBe.addAnswer(new Answer(newBe, newBe, cm.getAttribute("PRI_CODE"), beCode));
				newBe.addAnswer(new Answer(newBe, newBe, cm.getAttribute("PRI_HTML_MERGE"),
						beaUtils.getEntityAttribute(
								productCode, beCode, "PRI_HTML_MERGE").getValueString()));

				beUtils.updateBaseEntity(newBe);
			} else {
				log.error("Fetched null baseentity");
			}
		}
		return true;
	}

}
