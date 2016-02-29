package blaze.athena.controller;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Chang Kon Han
 * @author John Law
 * @author Wesley Yep
 * @since 29 Feb 2016
 */
@Controller
public class GreetingController {

    /**
     * <p>Binds ALL HTTP requests to /greeting to this methods</p>
     * @param name
     * @param model
     * @return
     */

    @RequestMapping("/greeting")
    public String greeting(@RequestParam(value="name", required=false, defaultValue="World") String name, Model model) {
        // add the value name to the model object. makes it accessible to the view template
        model.addAttribute("name", name);
        return "greeting";
    }

}
