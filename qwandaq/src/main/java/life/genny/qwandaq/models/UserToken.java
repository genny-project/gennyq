package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.datatype.Capability;
import life.genny.qwandaq.entity.BaseEntity;

import java.util.Set;

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

	private Set<Capability> capabilities;

	public UserToken() { }

	public UserToken(final String code, final String token) {
		super(code, token);
		capabilities = capMan.getUserCapabilities();
	}

	public UserToken(final String token) {
		super(token);
		capabilities = capMan.getUserCapabilities();
	}

    @PostConstruct
    void init() {
		log.debug("CONSTRUCTING UserToken Bean");
    }

    @PreDestroy
    void destroy() {
		log.debug("DESTROYING UserToken Bean");
    }

	/**
	 * Return the user base entity (Entity with code PER_ + this token's uuid)
	 * @return
	 */
	public BaseEntity getUserEntity() {
		return beUtils.getBaseEntity(GennyConstants.PER_BE_PREFIX.concat(getUuid()));
	}

	/**
	 * Get this user's capabilities
	 * @return
	 */
	public Set<Capability> getUserCapabilities() {
		return capabilities;
	}

}
