package life.genny.messages.managers;


import java.util.Map;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.BaseEntityUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.qwandaq.models.UserToken;

@ApplicationScoped
public abstract class QMessageProvider {
	
	@Inject
	protected BaseEntityUtils beUtils;

	@Inject
	protected UserToken userToken;

	protected static Jsonb jsonb = JsonbBuilder.create();

	public QMessageProvider() {
		System.err.println("[!] Inside thread inside message provider: " + (beUtils!=null));
	}


	public abstract void sendMessage(BaseEntity templateBe, Map<String, Object> contextMap);

}
