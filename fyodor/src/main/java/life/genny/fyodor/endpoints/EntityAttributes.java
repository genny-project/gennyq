package life.genny.fyodor.endpoints;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
     * Fetch an EntityAttribute from a given product using a baseEntityCode and attributeCode, without its DataType
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @return <ul>
     *             <li>200 OK with the EntityAttribute if found.</li>
     *             <li>404 with an associated error message if the EntityAttribute cannot be found/li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{baseEntityCode}/{attributeCode}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("baseEntityCode") String baseEntityCode,
            @PathParam("attributeCode") String attributeCode) {
                return fetch(product, baseEntityCode, attributeCode, "false");
            }

    /**
     * Fetch an EntityAttribute from a given product using a baseEntityCode and attributeCode and optionally its attribute, but not datatype
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @param embedAttr - whether or not to embed the attribute in the Entity Attribute
     * @return <ul>
     *             <li>200 OK with the EntityAttribute if found.</li>
     *             <li>404 with an associated error message if the EntityAttribute or its Attribute cannot be found</li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{baseEntityCode}/{attributeCode}/{embedAttr}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("baseEntityCode") String baseEntityCode,
            @PathParam("attributeCode") String attributeCode,
            @PathParam("embedAttr") String embedAttr) {
                return fetch(product, baseEntityCode, attributeCode, embedAttr, "false");
            }

    /**
     * Fetch an EntityAttribute from a given product using a baseEntityCode and attributeCode with optionally its attribute and data type, but not validation list
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @param embedAttr - whether or not to embed the attribute in the Entity Attribute
     * @param embedDtt - whether or not to embed the data type
     * @return <ul>
     *             <li>200 OK with the EntityAttribute if found.</li>
     *             <li>404 with an associated error message if the EntityAttribute or any of its requested components cannot be found</li>
     *          </ul>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{baseEntityCode}/{attributeCode}/{embedAttr}/{embedDtt}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("baseEntityCode") String baseEntityCode,
            @PathParam("attributeCode") String attributeCode,
            @PathParam("embedAttr") String embedAttr,
            @PathParam("embedDtt") String embedDtt) {
                return fetch(product, baseEntityCode, attributeCode, embedAttr, embedDtt, "false");
            }

    /**
     * Fetch an EntityAttribute from a given product using a baseEntityCode and attributeCode with optionally its attribute, data type and validation list
     * <p>All boolean values in this: if !value.equalsIgnoreCase("true") then it is assumed they are false
     * @param product - product to fetch from
     * @param baseEntityCode - base entity code of the Entity Attribute
     * @param attributeCode - attribute code of the Attribute
     * @param embedAttrStr - whether or not to embed the attribute in the Entity Attribute
     * @param embedDttStr - whether or not to embed the data type
     * @param embedVldStr - whether or not to embed the Validation
     * @return <ul>
     *             <li>200 OK with the EntityAttribute if found.</li>
     *             <li>404 with an associated error message if the EntityAttribute or any of its requested components cannot be found</li>
     *          </ul>
     * 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{product}/{baseEntityCode}/{attributeCode}/{embedAttr}/{embedDtt}/{embedVld}")
    public Response fetch(
            @PathParam("product") String product, 
            @PathParam("baseEntityCode") String baseEntityCode,
            @PathParam("attributeCode") String attributeCode,
            @PathParam("embedAttr") String embedAttrStr,
            @PathParam("embedDtt") String embedDttStr,
            @PathParam("embedVld") String embedVldStr) {
                log.debug("[!] call to GET /entityattributes/" + product + "/" + baseEntityCode + "/" + attributeCode);

                if (userToken == null) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("Not authorized to make this request").build();
                }
                
                boolean embedAttr = "true".equalsIgnoreCase(embedAttrStr);
                boolean embedDtt = "true".equalsIgnoreCase(embedDttStr);
                boolean embedVld = "true".equalsIgnoreCase(embedVldStr);

                try {
                    EntityAttribute attribute = beaUtils.getEntityAttribute(product, baseEntityCode, attributeCode, embedAttr, embedDtt, embedVld);
                    log.info("Found EA: " + (attribute.getBaseEntityCode() != null ? attribute.getBaseEntityCode() : "null") + ":" + (attribute.getAttributeCode() != null ? attribute.getAttributeCode() : "null"));
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
    @Produces(MediaType.APPLICATION_JSON)
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
                if(attribute != null) {
                    log.info("Successfully removed entity");
                    return Response.ok(attribute).entity("Successfully removed entity").build();
                }
                return Response.status(Status.NO_CONTENT).build();
                
            } catch(ItemNotFoundException e) {
                return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
            }
        }
}
