package blaze.athena.controller;

import blaze.athena.account.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;

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

    private final AccountRepository accountRepository;

    @Inject
    public DashboardController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName(); //get logged in username

        String id = accountRepository.findIdbyUserName(name);
        System.out.println(id);
        model.addAttribute("username", id);
        return "dashboard";
    }

}
