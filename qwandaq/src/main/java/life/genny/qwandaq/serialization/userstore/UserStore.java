package life.genny.qwandaq.serialization.userstore;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.time.LocalDateTime;

/*
 * A representation of BaseEntity in the cache
 * 
 * @author Varun Shastry
 */
public class UserStore implements CoreEntitySerializable {

	@ProtoField(1)
	private String realm;

	@ProtoField(2)
	private String usercode;

	@ProtoField(3)
	private String jtiAccess;

	@ProtoField(4)
	private LocalDateTime lastActive;

	@ProtoFactory
	public UserStore(String realm, String usercode, String jtiAccess, LocalDateTime lastActive) {
		super();
		this.realm = realm;
		this.usercode = usercode;
		this.jtiAccess = jtiAccess;
		this.lastActive = lastActive;
	}

	public UserStore() {
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public String getUsercode() {
		return usercode;
	}

	public void setUsercode(String usercode) {
		this.usercode = usercode;
	}

	public String getJtiAccess() {
		return jtiAccess;
	}

	public void setJtiAccess(String jtiAccess) {
		this.jtiAccess = jtiAccess;
	}

	public LocalDateTime getLastActive() {
		return lastActive;
	}

	public void setLastActive(LocalDateTime lastActive) {
		this.lastActive = lastActive;
	}

	@Override
	public CoreEntityPersistable toPersistableCoreEntity() {
		throw new UnsupportedOperationException();
	}

}
