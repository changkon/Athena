package blaze.athena.services;



import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by John on 7/04/2016.
 */
@Path("/testdb")
public interface ITestDbResource {

    @POST
    @Path("/upload")
    @Consumes("application/json")
    Response createANewQuestion();

}
