package life.genny.qwandaq.utils;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.constants.GennyConstants;
import life.genny.qwandaq.serialization.CoreEntitySerializable;
import life.genny.qwandaq.serialization.userstore.UserStore;
import life.genny.qwandaq.serialization.userstore.UserStoreKey;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;

/**
 * A non-static utility class used for standard
 * operations involving user login session information.
 *
 * @author Varun Shastry
 */
@ApplicationScoped
@ActivateRequestContext
public class UserStoreUtils {
    private static final Logger log = Logger.getLogger(UserStoreUtils.class);

    public CoreEntitySerializable getSerializableUserStore(String productCode, String usercode) {
        UserStoreKey key = new UserStoreKey(productCode, usercode);
        return CacheUtils.getSerializableEntity(GennyConstants.CACHE_NAME_USERSTORE, key);
    }

    public CoreEntityPersistable getPersistableUserStore(String productCode, String usercode) {
        UserStoreKey key = new UserStoreKey(productCode, usercode);
        return CacheUtils.getPersistableEntity(GennyConstants.CACHE_NAME_USERSTORE, key);
    }

    public void updateSerializableUserStore(UserStore userStore) {
        UserStoreKey key = new UserStoreKey(userStore.getRealm(), userStore.getUsercode());
        CacheUtils.saveEntity(GennyConstants.CACHE_NAME_USERSTORE, key, userStore.toPersistableCoreEntity());
    }
}