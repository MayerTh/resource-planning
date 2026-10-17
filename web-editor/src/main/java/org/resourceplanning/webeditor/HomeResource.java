package org.resourceplanning.webeditor;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Location;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class HomeResource {

    @Inject
    @Location("home/index.html")
    Template indexTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return indexTemplate.instance();
    }
}
