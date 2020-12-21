package com.dappcraft;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/entities")
@RegisterRestClient
public interface DclApiService {
    @GET
    @Path("/profiles")
    List<DclUserInfo> getUserInfo(@QueryParam("pointer") String address);
}
