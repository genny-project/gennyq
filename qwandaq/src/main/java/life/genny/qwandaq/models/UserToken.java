package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

import org.jboss.logging.Logger;

/**
 * An extension of the GennyToken clas that represents the user
 *
 * Annotated with RequestScoped to ensure a more transient state 
 * with access that lasts only as long as the http request or 
 * kafka consumption flow is active.
 **/
@RegisterForReflection
@RequestScoped
public class UserToken extends GennyToken {

	private static final long serialVersionUID = 1L;
	static final Logger log = Logger.getLogger(UserToken.class);

	public UserToken() { }

	public UserToken(final String code, final String token) {
		super(code, token);
	}

	public UserToken(final String token) {
		super(token);
	}

    @PostConstruct
    void init() {
		log.debug("CONSTRUCTING UserToken Bean");
    }

    @PreDestroy
    void destroy() {
		log.debug("DESTROYING UserToken Bean");
    }

}
