package org.resourceplanning.webeditor;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.resourceplanning.io.DataValidationException;

@Provider
public class DataValidationExceptionMapper implements ExceptionMapper<DataValidationException> {

    @Override
    public Response toResponse(DataValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN)
            .entity(exception.getMessage())
            .build();
    }
}
