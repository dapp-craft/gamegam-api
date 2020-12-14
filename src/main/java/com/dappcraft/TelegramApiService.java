package com.dappcraft;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/telegram")
@RegisterRestClient
public interface TelegramApiService {

    @GET
    @Path("/check_user_in_group/{groupName}/{userName}")
    CheckResult check(@PathParam String groupName, @PathParam String userName);
}
