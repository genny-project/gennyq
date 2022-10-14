package life.genny.gadaq.utils;

import io.smallrye.common.vertx.ContextLocals;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class CDIContext {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass());
    public static final String TOKEN = "TOKEN";

    public CDIContext() {
    }

    public static void putContextToken(String token) {
        ContextLocals.put(TOKEN, token);
    }

    public static String getContextToken() {
        String tokenVal = "";
        Optional<String> token = ContextLocals.get(TOKEN);
        if(token.isPresent()) {
            return token.get();
        }
        return tokenVal;
    }

}
