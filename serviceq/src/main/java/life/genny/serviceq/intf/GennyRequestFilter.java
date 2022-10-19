package life.genny.serviceq.intf;

import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.inject.Inject;

import life.genny.qwandaq.kafka.KafkaTopic;
import life.genny.qwandaq.message.JtiMessage;
import life.genny.qwandaq.utils.KafkaUtils;
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

			//TODO: JTI
			sendJtiMessage(token, requestContext.getHeaders());

			log.debug("Token Initialized: " + userToken);

		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	public void sendJtiMessage(String token,MultivaluedMap<String,String> headers) {
		JtiMessage JtiMessage = new JtiMessage();
		String userAgent =  headers.getFirst(HttpHeaders.USER_AGENT);
		JtiMessage.setToken(token);
		JtiMessage.setMessage(userAgent);

		KafkaUtils.writeMsg(KafkaTopic.JTI_EVENTS, JtiMessage);
	}
}
