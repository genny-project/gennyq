package life.genny.qwandaq.managers;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.utils.EntityAttributeUtils;
import life.genny.qwandaq.utils.QuestionUtils;
import life.genny.qwandaq.models.ServiceToken;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public abstract class Manager {
	protected static Jsonb jsonb = JsonbBuilder.create();

	@Inject
	protected DefUtils defUtils;

	@Inject
	protected UserToken userToken;

	@Inject
	protected ServiceToken serviceToken;

	@Inject
	protected QwandaUtils qwandaUtils;

	@Inject
	protected BaseEntityUtils beUtils;

	@Inject
	protected EntityAttributeUtils beaUtils;


	@Inject
	protected QuestionUtils questionUtils;

	@Inject
	protected AttributeUtils attributeUtils;
	
	@Inject
	protected CacheManager cm;

    protected String className() {
		String str = this.getClass().getSimpleName();
		int index = str.indexOf('_');
		if(index != -1)
			str = str.substring(0, index);
		return str != null ? str : MethodHandles.lookup().lookupClass().getCanonicalName();
	}
}
