package life.genny.bridge.endpoints;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.vertx.core.http.HttpServerRequest;
import life.genny.bridge.live.data.BridgeGrpcService;
import life.genny.bridge.model.grpc.Item;

/**
 * This is a test endpoint for working with GRPC.
 * DO NOT COMMIT TO THE FINAL VERSION
 * 
 * @author Dan
 */
@Path("/api/grpctest")
public class GrpcTestEndpoint {

    @Context
	UriInfo uriInfo;

    @Context
	HttpServerRequest request;

    @Inject
    BridgeGrpcService grpcService;

    @GET
    @Path("/{message}")
    public String sendGrpcData(@PathParam("message") String message) {

        grpcService.broadcast(Item.newBuilder().setBody(message).build());

        return "Sending!";
    }
    
}
