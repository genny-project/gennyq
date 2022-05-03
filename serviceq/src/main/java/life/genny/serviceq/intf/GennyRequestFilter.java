package life.genny.serviceq.intf;

import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import org.jboss.logging.Logger;

import life.genny.qwandaq.models.UserToken;

/**
 * Custom request filter for initializing the UserToken on Http requests.
 **/
@Provider
public class GennyRequestFilter implements ContainerRequestFilter {

	static final Logger log = Logger.getLogger(GennyRequestFilter.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	UserToken userToken;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String token = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (token == null) {
			// log.warn("No Authorization header sent in request!");
			return;
		}

		token = StringUtils.removeStart(token, "Bearer ");

		try {
			// build GennyToken from token string in headers
			userToken.init(token);
			log.info("Token Initialized: " + userToken);

		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
