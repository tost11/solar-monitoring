package de.tostsoft.solarmonitoring.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {
    @GetMapping("/**/{path:[^\\.]*}")
    public String redirect() {
        return "forward:/";
    }
}
