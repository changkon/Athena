package blaze.athena.application;

import blaze.athena.services.HelloResource;
import blaze.athena.services.PDFResource;
import blaze.athena.services.QuestionResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Entry point into REST service for Athena</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 23 Feb 2016
 */
@ApplicationPath("/services")
public class AthenaRestApplication extends Application {

    public static final String BASEURI = "http://localhost:10003/services";

    private Set<Class<?>> components = new HashSet<Class<?>>();

    public AthenaRestApplication() {
        components.add(HelloResource.class);
        components.add(PDFResource.class);
        components.add(QuestionResource.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return components;
    }
}
