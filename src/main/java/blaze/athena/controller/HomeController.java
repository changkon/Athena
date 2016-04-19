package blaze.athena.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by John on 19/04/2016.
 */
@Controller
public class HomeController {


    @RequestMapping("/")
    public String dashboard(Model model) {

        return "redirect:dashboard";
    }
}
