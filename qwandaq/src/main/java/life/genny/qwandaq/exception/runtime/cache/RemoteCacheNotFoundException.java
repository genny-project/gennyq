package life.genny.qwandaq.exception.runtime.cache;

import life.genny.qwandaq.exception.GennyRuntimeException;

public class RemoteCacheNotFoundException extends GennyRuntimeException {
    

    // TODO: Replace all occurrences of realm in gennyq with product code
    public RemoteCacheNotFoundException(String realm) {
        super("Remote Cache Not Found: " + realm);
    }
}
