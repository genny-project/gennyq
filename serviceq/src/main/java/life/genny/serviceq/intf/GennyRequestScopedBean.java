package life.genny.serviceq.intf;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.models.TokenCollection;

@RequestScoped
public class GennyRequestScopedBean {

	static final Logger log = Logger.getLogger(GennyDeserializer.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Context
	HttpHeaders httpHeaders;

	@Inject
	TokenCollection tokens;

    @PostConstruct
    void init() {

		String token = httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION);

		if (token == null) {
			log.warn("No Authorization header sent in request!");
			return;
		}

		token = StringUtils.removeStart(token, "Bearer ");

		GennyToken gennyToken = null;
		try {
			// build GennyToken from token string in headers
			gennyToken = new GennyToken(token);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// update our gennyToken item
		tokens.setGennyToken(gennyToken);
		log.info("Token Initialized: " + gennyToken);
    }
}
