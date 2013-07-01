package com.tesco.services.resources;

import com.google.common.base.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.IOException;

import static com.google.common.io.Files.readLines;
import static com.tesco.services.HTTPResponses.*;
import static java.nio.charset.Charset.defaultCharset;

@Path("/price/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {

    @GET
    public Response get(@Context UriInfo uriInfo){
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        Optional<String> callback = Optional.fromNullable(queryParameters.getFirst("callback"));

        try {
            String version = readLines(new File("version"), defaultCharset()).get(0);
            String versionJson = String.format("{\"version\": \"%s\"}", version);

            if (callback.isPresent()) {
                return ok((callback.get() + "(" + versionJson + ")"));
            }
            return ok(versionJson);

        } catch (IOException e) {
            System.out.println(e.toString());
            return notFound(e.getMessage());
        }

    }
}
