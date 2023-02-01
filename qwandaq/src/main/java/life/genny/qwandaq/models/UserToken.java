package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.datatype.capability.core.Capability;
import life.genny.qwandaq.entity.BaseEntity;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.json.bind.annotation.JsonbTransient;

import org.jboss.logging.Logger;

/**
 * An extension of the GennyToken clas that represents the user
 *
 * Annotated with RequestScoped to ensure a more transient state 
 * with access that lasts only as long as the http request or 
 * kafka consumption flow is active.
 **/
@RegisterForReflection
// @RequestScoped
// TODO: REMOVE THIS AFTER DONE!!!
@ApplicationScoped
public class UserToken extends GennyToken {

	private static final long serialVersionUID = 1L;
	static final Logger log = Logger.getLogger(UserToken.class);

	@JsonbTransient
	private Set<Capability> capabilities;

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
		log.debug("Initializing user capabilities");
    }

    @PreDestroy
    void destroy() {
		log.debug("DESTROYING UserToken Bean");
    }
}
