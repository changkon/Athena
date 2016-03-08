package blaze.athena.services;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.springframework.http.ResponseEntity;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 01 Mar 2016
 */
public interface IDocumentResource {

    @POST
    @Path("/upload")
    @Consumes("multipart/form-data")
    Response uploadFile(@MultipartForm MultipartFormDataInput input);

    @POST
    @Path("/generate")
    @Consumes("multipart/form-data")
    @Produces("application/json")
    ResponseEntity generateQuestions(@MultipartForm MultipartFormDataInput input);

    default File getFile(byte[] content, String filename) throws IOException {
        File file = new File(filename);

        try (FileOutputStream fop = new FileOutputStream(file)) {
            fop.write(content);
        }

        return file;
    }

}
