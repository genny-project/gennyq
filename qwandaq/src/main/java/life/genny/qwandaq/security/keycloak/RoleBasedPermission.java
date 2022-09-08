package life.genny.qwandaq.security.keycloak;

import life.genny.qwandaq.models.GennyToken;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Arrays;

/**
 * RoleBasedPermission --- This class is composed with Authentication 
 * The methods below are for checking if the user has the right roles 
 * and permissions within the token payload.
 * @author    hello@gada.io
 *
 */
@Singleton
public class RoleBasedPermission {

    static final Logger LOG = Logger.getLogger(RoleBasedPermission.class);

	static Jsonb jsonb = JsonbBuilder.create();

    @Inject TokenVerification verification;

    /**
     * Similar to RolesAllowed annotation used in restful methods. It will 
     * check if the array of roles passed in the second and subsequent 
     * parameters are contained within the payload token. It will suffice 
     * to have only one role matched. 
     *
     * @param gennyToken {@link GennyToken}
     * @param roles String array of roles 
     *
     * @return True if a role is matched within the roles of the payload
     *         False otherwise
     */
    public Boolean rolesAllowed(GennyToken gennyToken, String...roles) {
		
        try {
            gennyToken = verification.verify(gennyToken.getKeycloakRealm(), gennyToken.getToken());
		} catch (Exception e) {
			e.printStackTrace();
		}

        return Arrays.stream(roles)
                .filter(gennyToken.getUserRoles()::contains)
                .map(d -> true)
                .findAny()
                .orElse(false);
    }

}
