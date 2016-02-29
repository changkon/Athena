package blaze.athena.services;

import javax.ws.rs.*;

/**
 * <p>Example resource which returns a simple Hello message</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
@Path("/pdf")
public interface IPdfResource {

    /**
     * <p>Returns a hello message</p>
     * @return Hello Message
     */

    @GET
    @Produces("text/plain")
    String test();

}
