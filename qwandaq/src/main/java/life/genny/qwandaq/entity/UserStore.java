package life.genny.qwandaq.entity;

import life.genny.qwandaq.CodedEntity;
import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.serialization.CoreEntitySerializable;

import java.time.Instant;
import java.time.LocalDateTime;

/*
 * A representation of UserStore in the database
 *
 * @author Varun Shastry
 */
public class UserStore extends CodedEntity implements CoreEntityPersistable {

    private String realm;

    private String usercode;

    private String jtiAccess;

    private long lastActive;

    public UserStore(String realm, String usercode, String jtiAccess) {
        super();
        this.realm = realm;
        this.usercode = usercode;
        this.jtiAccess = jtiAccess;
        this.lastActive = Instant.now().getEpochSecond();
        this.setCode(realm + "-" + this.usercode);
        this.setName(realm + "-" + this.usercode);
    }

    public UserStore(String realm, String usercode, String jtiAccess, long lastActive) {
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

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    @Override
    public CoreEntitySerializable toSerializableCoreEntity() {
        return new life.genny.qwandaq.serialization.userstore.UserStore(realm,usercode,jtiAccess,lastActive);
    }
}
