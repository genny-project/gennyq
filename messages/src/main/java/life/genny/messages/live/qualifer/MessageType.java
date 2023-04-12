package life.genny.messages.live.qualifer;


import life.genny.qwandaq.message.QBaseMSGMessageType;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface MessageType {

    QBaseMSGMessageType type();
}
