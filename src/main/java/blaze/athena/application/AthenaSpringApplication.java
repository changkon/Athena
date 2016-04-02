package blaze.athena.application;

import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.servlet.Filter;
import javax.servlet.ServletContextListener;

/**
 * <p>Entry point into Spring boot application</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 29 Feb 2016
 */
@SpringBootApplication
@ComponentScan({"blaze.athena"})
@EnableAutoConfiguration
public class AthenaSpringApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(AthenaSpringApplication.class, args);
    }

    /**
     * <p>Overrides the method to allow configuration of application when launched by the servlet container</p>
     * @param application
     * @return
     */

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AthenaSpringApplication.class);
    }

    @Bean
    public FilterRegistrationBean filterRegistration() {
        FilterRegistrationBean insightFilter = new FilterRegistrationBean();

        insightFilter.setFilter(applicationInsightFilter());
        insightFilter.addUrlPatterns("/*");
        insightFilter.setName("ApplicationInsightsWebFilter");

        return insightFilter;
    }

    @Bean(name = "ApplicationInsightsWebFilter")
    public Filter applicationInsightFilter() {
        return new WebRequestTrackingFilter();
    }

    @Bean
    public ServletContextInitializer initializer() {
        return servletContext -> {
            // Resteasy configuration
            servletContext.setInitParameter("resteasy.scan", "true");
            servletContext.setInitParameter("resteasy.servlet.mapping.prefix", "/services");
        };
    }

    @Bean
    public ServletContextListener restEasyBootstrap() {
        return new ResteasyBootstrap();
    }

    @Bean
    public ServletRegistrationBean restEasyServlet() {
        final ServletRegistrationBean registrationBean = new ServletRegistrationBean();
        registrationBean.setServlet(new HttpServletDispatcher());
        registrationBean.setName("athena-resteasy");
        registrationBean.addUrlMappings("/services/*");
        registrationBean.addInitParameter("javax.ws.rs.Application", "blaze.athena.application.AthenaRestApplication");
        return registrationBean;
    }

}
