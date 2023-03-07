package life.genny.fyodor.endpoints;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;

import life.genny.qwandaq.CoreEntityPersistable;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.exception.runtime.ItemNotFoundException;
import life.genny.qwandaq.managers.CacheManager;
import life.genny.qwandaq.models.UserToken;
import life.genny.qwandaq.serialization.entityattribute.EntityAttributeKey;
import life.genny.qwandaq.utils.EntityAttributeUtils;

/**
 * Endpoint for modifying EntityAttributes
 */
@Path("entityattributes")
public class EntityAttributes {
    
    @Inject
    UserToken userToken;
    
    @Inject
    EntityAttributeUtils beaUtils;

    @Inject
    Logger log;

    @Inject
    CacheManager cm;

    /**
     * Fetch an EntityAttribute from a given product using a baseEntityCode and attributeCode
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @return 200 OK with the EntityAttribute if found. 404 with an associated error message if the EntityAttribute cannot be found
     * 
     */
    @GET
    @Path("/{product}/{baseEntityCode}/{attributeCode}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("baseEntityCode") String baseEntityCode,
            @PathParam("attributeCode") String attributeCode) {
                log.debug("[!] call to GET /entityattributes/" + product + "/" + baseEntityCode + "/" + attributeCode);

                if (userToken == null) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Not authorized to make this request").build();
                }
        
                try {
                    EntityAttribute attribute = beaUtils.getEntityAttribute(product, baseEntityCode, attributeCode, true, true, true);
                    return Response.ok(attribute).build();
                } catch(ItemNotFoundException e) {
                    return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
                }
            }

    /**
     * Delete an EntityAttribute from a given product using a baseEntityCode and attributeCode
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @return 200 OK with the EntityAttribute if found. 404 with an associated error message
     */
    @DELETE
    @Path("/{product}/{baseEntityCode}/{attributeCode}")
    public Response delete(
        @PathParam("product") String product, 
        @PathParam("baseEntityCode") String baseEntityCode,
        @PathParam("attributeCode") String attributeCode) {
            log.debug("[!] call to DELETE /entityattributes/" + product + "/" + baseEntityCode + "/" + attributeCode);

            if (userToken == null) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Not authorized to make this request").build();
            }
    
            try {
                EntityAttributeKey key = new EntityAttributeKey(product, baseEntityCode, attributeCode);
                CoreEntityPersistable attribute = cm.removePersistableEntity(product, key);
                
                return Response.ok(attribute).build();
            } catch(ItemNotFoundException e) {
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            }
        }
}
