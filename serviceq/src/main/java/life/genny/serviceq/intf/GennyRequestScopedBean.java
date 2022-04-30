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

		token = StringUtils.removeStart(token, "Bearer ");

		GennyToken gennyToken = new GennyToken(token);
		tokens.setGennyToken(gennyToken);
    }
}
