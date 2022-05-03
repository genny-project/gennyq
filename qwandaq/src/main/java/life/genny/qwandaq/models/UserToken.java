package life.genny.qwandaq.models;

import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

import org.jboss.logging.Logger;

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
		log.info("CONSTRUCTING UserToken Bean");
    }

    @PreDestroy
    void destroy() {
		log.info("DESTROYING UserToken Bean");
    }

}
