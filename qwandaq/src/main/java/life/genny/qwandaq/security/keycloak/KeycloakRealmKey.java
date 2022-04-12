package life.genny.qwandaq.security.keycloak;

import java.math.BigInteger;

import javax.json.bind.annotation.JsonbProperty;

import org.jose4j.base64url.Base64Url;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * KeycloakRealmKey --- An object of this type will have the necessary 
 * information to construct the public key and verify a token signature 
 * to confirm if it was signed by the right Authorization server.
 *
 * @author    hello@gada.io
 *
 */
@RegisterForReflection
public class KeycloakRealmKey {

    /* Key id */
    public String kid; 
    /* Algorithm such as SH256 */
    public String kty;
    /* Algorithm type such as HS256 */
    public String alg;
    public String sig;

	@JsonbProperty("n")
    public String modulus;
	@JsonbProperty("e")
    public String exponent;

	public KeycloakRealmKey() { }

    @Override
    public String toString() {
        return "KeycloakRealmKey [alg=" + alg + ", exponent=" + exponent + ", kid=" + kid + ", kty=" + kty
                + ", modulus=" + modulus + ", sig=" + sig + "]";
    }

	public void setKid(String kid) {
		this.kid = kid;
	}

	public String getKid() {
		return kid;
	}

	public void setKty(String kty) {
		this.kty = kty;
	}

	public String getKty() {
		return kty;
	}

	public void setAlg(String alg) {
		this.alg = alg;
	}

	public String getAlg() {
		return alg;
	}

	public void setSig(String sig) {
		this.sig = sig;
	}

	public String getSig() {
		return sig;
	}

	public void setModulus(String modulus) {
		this.modulus = modulus;
	}

	public String getModulus() {
		return modulus;
	}

	public void setExponent(String exponent) {
		this.exponent = exponent;
	}

	public String getExponent() {
		return exponent;
	}

	public BigInteger getDecodedModulus() {
        return new BigInteger(1, Base64Url.decode(this.modulus));
	}

	public BigInteger getDecodedExponent() {
        return new BigInteger(1, Base64Url.decode(this.exponent));
	}
}

