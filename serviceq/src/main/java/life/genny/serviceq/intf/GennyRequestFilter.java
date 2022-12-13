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

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Logger log;

	@Inject
	UserToken userToken;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

		// grab auth header
        String token = requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (token == null)
			return;

		// strip token string
		token = StringUtils.removeStart(token, "Bearer ");

		// build GennyToken from token string in headers
		userToken.init(token);
		log.debug("Token Initialized: " + userToken);
	}

}
