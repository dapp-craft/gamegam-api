package com.dappcraft;

import com.dappcraft.broxus.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.ws.rs.*;
import java.util.List;
import java.util.Set;

@Path("/v1")
@RegisterRestClient
public interface BroxusService {

    @GET
    @Path("/workspaces")
    List<Workspace> workspaces(@HeaderParam("api-key") String apiKey);
    @POST
    @Path("/users/balances")
    List<Balance> usersBalances(@HeaderParam("api-key") String apiKey,
                                @HeaderParam("nonce") String nonce,
                                @HeaderParam("sign") String sign,
                                String data);
    @POST
    @Path("/transfer")
    ResponseTransfer transfer(@HeaderParam("api-key") String apiKey,
                              @HeaderParam("nonce") String nonce,
                              @HeaderParam("sign") String sign,
                              String data);
    @POST
    @Path("/static_addresses/renew")
    ResponseAddressesRenew addressesRenew(@HeaderParam("api-key") String apiKey,
                                          @HeaderParam("nonce") String nonce,
                                          @HeaderParam("sign") String sign,
                                          String data);
}
