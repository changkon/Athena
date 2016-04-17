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
@Path("/question")
public interface IQuestionResource {

    @POST
    @Path("/store")
    @Consumes("application/json")
    @Produces("text/plain")
    String storeQuestion(@RequestBody QuestionDTO input);

    @POST
    @Path("/rate")
    @Consumes("application/json")
    @Produces("text/plain")
    String rateQuestion(@RequestBody QuestionDTO input);

    @GET
    @Path("/categories")
    @Produces("application/json")
    ResponseEntity getCategories();

}
