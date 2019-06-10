package com.browserup.bup;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Singleton
@Path("/foo")
@Produces(MediaType.APPLICATION_JSON)
public class FooResource {

    @Inject
    public FooResource() {
        System.out.println();
    }

    @POST
    public String handleFooPost(@QueryParam("bar") String bar, @QueryParam("quux") int quux) {
        return "{\"test\":\"test\"}";
    }

    @GET
    public String handleFooGet() {
        return "{\"test\":\"test\"}";
    }
}