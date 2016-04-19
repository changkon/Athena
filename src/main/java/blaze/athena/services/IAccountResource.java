package blaze.athena.services;

import blaze.athena.dto.QuestionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.*;

/**
 * <p>Example resource which returns a simple Hello message</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
@Path("/account")
public interface IAccountResource {

    @GET
    @Path("{id}")
    @Produces("application/json")
    ResponseEntity getAccountName(@PathParam("id") int accountId);

}
