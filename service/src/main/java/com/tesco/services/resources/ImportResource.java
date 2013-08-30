package com.tesco.services.resources;

import com.tesco.services.Configuration;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ahampson
 * Date: 19/08/2013
 * Time: 16:39
 * To change this template use File | Settings | File Templates.
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    private Configuration configuration;
    private RuntimeWrapper runtimeWrapper;

    public ImportResource(Configuration configuration, RuntimeWrapper runtimeWrapper) {
        this.configuration = configuration;
        this.runtimeWrapper = runtimeWrapper;
    }


    @GET
    @Path("/import")
    @Metered(name="postImport-Meter",group="PriceServices")
    @Timed(name="postImport-Timer",group="PriceServices")
    @ExceptionMetered(name="postImport-Failures",group="PriceServices")
    public Response getPIMHierarchy() {

        try {
            runtimeWrapper.exec(configuration.getImportScript());
        } catch (IOException e) {
            Response.serverError();
        }

        return Response.ok("Import Started.").build();
    }
}
