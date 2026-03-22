package app.juntos.alpha.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    
    @GetMapping("/")
    public String home() {
        return "ok";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
