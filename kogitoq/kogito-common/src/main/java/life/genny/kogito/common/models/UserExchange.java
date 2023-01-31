package life.genny.kogito.common.models;

import java.io.Serializable;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents an exchange of information between services for Task processing.
 * <p>
 * The state of the completion enum will depend on the nature of the 
 * task completion in ProcessQuestions. If the value is CUSTOM, then 
 * a completionCode is provided to assist in further narrowing down 
 * the kind of event that triggered completion.
 *
 * @author Adam Crow
 * @author Jasper Robison
 */
@RegisterForReflection
public class UserExchange implements Serializable {

	private String userCode;
	private String token;

	public UserExchange() {
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
