package life.genny.messages.managers;


import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.utils.AttributeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.EntityAttributeUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.Map;

@ApplicationScoped
public abstract class QMessageProvider  {

    @Inject
    protected AttributeUtils attributeUtils;
    
    @Inject
    protected BaseEntityUtils beUtils;

    @Inject
    protected EntityAttributeUtils beaUtils;

    @Inject
    protected UserToken userToken;

    protected static Jsonb jsonb = JsonbBuilder.create();

    public abstract void sendMessage(Map<String, Object> contextMap);

}