package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * An extension of the GennyToken clas that represents the service user.
 *
 * Annotated with ApplicationScoped to provide access throughout the service lifecycle.
 **/
@RegisterForReflection
@ApplicationScoped
public class ServiceToken extends GennyToken {

	private static final long serialVersionUID = 1L;
	static final Logger log = Logger.getLogger(ServiceToken.class);

	public ServiceToken() { }

	public ServiceToken(final String code, final String token) {
		super(code, token);
	}

	public ServiceToken(final String token) {
		super(token);
	}

    @PostConstruct
    void init() {
		log.debug("CONSTRUCTING ServiceToken Bean");
    }

    @PreDestroy
    void destroy() {
		log.debug("DESTROYING ServiceToken Bean");
    }

}
