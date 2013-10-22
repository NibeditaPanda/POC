package com.tesco.services.resources;

import com.tesco.services.Configuration;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.Response.ok;

@Path("/admin")
@Produces(ResourceResponse.RESPONSE_TYPE)
public class ImportResource {
    private Configuration configuration;
    private RuntimeWrapper runtimeWrapper;

    public ImportResource(Configuration configuration, RuntimeWrapper runtimeWrapper) {
        this.configuration = configuration;
        this.runtimeWrapper = runtimeWrapper;
    }

    @POST
    @Path("/import")
    @Metered(name = "postImport-Meter", group = "PriceServices")
    @Timed(name = "postImport-Timer", group = "PriceServices")
    @ExceptionMetered(name = "postImport-Failures", group = "PriceServices")
    public Response importData() {

        try {
            runtimeWrapper.exec(configuration.getImportScript());
        } catch (IOException e) {
            Response.serverError();
        }

        return ok("{\"message\":\"Import Started.\"}").build();
    }
}
