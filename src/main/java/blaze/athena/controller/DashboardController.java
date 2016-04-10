package blaze.athena.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * <p>Primary page for Athena web app. Dashboard is the interface users will use to generate questions</p>
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 06 Mar 2016
 */
@Controller
public class DashboardController {

    /**
     * <p>Binds ALL HTTP requests to /dashboard to this methods</p>
     * @param model
     * @return
     */

    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        return "dashboard";
    }

}
