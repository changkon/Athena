package blaze.athena.services;

import blaze.athena.dto.GroupDTO;
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
@Path("/group")
public interface IGroupResource {

    @POST
    @Path("/create")
    @Consumes("application/json")
    @Produces("text/plain")
    String createGroup(@RequestBody GroupDTO input);

    @GET
    @Path("{id}")
    @Produces("application/json")
    ResponseEntity getGroups(@PathParam("id") int ownerId);

}
