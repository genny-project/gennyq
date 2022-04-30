package life.genny.qwandaq.models;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@ApplicationScoped
public class TokenCollection {

	private static final long serialVersionUID = 1L;
	static final Logger log = Logger.getLogger(TokenCollection.class);
	static Jsonb jsonb = JsonbBuilder.create();

	public GennyToken gennyToken;
	public GennyToken serviceToken;

	public TokenCollection() { }

	public TokenCollection(GennyToken gennyToken, GennyToken serviceToken) {
		this.gennyToken = gennyToken;
		this.serviceToken = serviceToken;
	}

	public GennyToken getGennyToken() {
		return gennyToken;
	}

	public void setGennyToken(GennyToken gennyToken) {
		this.gennyToken = gennyToken;
	}

	public GennyToken getServiceToken() {
		return serviceToken;
	}

	public void setServiceToken(GennyToken serviceToken) {
		this.serviceToken = serviceToken;
	}

}
