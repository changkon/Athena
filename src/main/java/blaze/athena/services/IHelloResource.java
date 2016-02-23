package blaze.athena.services;

import javax.ws.rs.*;

/**
 * <p>Example resource which returns a simple Hello message</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
@Path("/hello")
public interface IHelloResource {

    /**
     * <p>Returns a hello message</p>
     * @param name
     * @return Hello Message
     */

    @GET
    @Produces("text/plain")
    String hello(@DefaultValue("World!") @QueryParam("name") String name);

}
