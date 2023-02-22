package life.genny.kogito.common.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import life.genny.qwandaq.utils.*;

import life.genny.kogito.common.core.Dispatch;
import life.genny.kogito.common.core.ProcessAnswers;
import life.genny.kogito.common.utils.KogitoUtils;
import life.genny.qwandaq.capabilities.RoleManager;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.serviceq.intf.GennyScopeInit;


/**
 * Class to hold bare essentials for bringing up classes that contain service task methods for Kogito Workflows
 */
@ApplicationScoped
public abstract class KogitoService {

	// one instance for all classes
	protected static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	ServiceToken serviceToken;

	@Inject
	UserToken userToken;

	@Inject
	BaseEntityUtils beUtils;

	@Inject
	EntityAttributeUtils beaUtils;

	@Inject
	KogitoUtils kogitoUtils;

	@Inject
	QwandaUtils qwandaUtils;

	@Inject
	DefUtils defUtils;

    @Inject
    FilterUtils filterUtils;

    @Inject
    SearchService search;
	
    @Inject
    SearchUtils searchUtils;

    @Inject
    Dispatch dispatch;

	@Inject
	DatabaseUtils databaseUtils;

	@Inject
	RoleManager roleManager;

	@Inject
	TaskService tasks;

	@Inject
	SearchService searchService;

	@Inject
	GennyScopeInit scope;

	@Inject
	ProcessAnswers processAnswers;

	@Inject
	NavigationService navigationService;

	@Inject
	EntityManager entityManager;

	@Inject
	AttributeUtils attributeUtils;
}
