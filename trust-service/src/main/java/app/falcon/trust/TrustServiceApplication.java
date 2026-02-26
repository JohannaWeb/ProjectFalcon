package app.falcon.trust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "app.falcon")
public class TrustServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrustServiceApplication.class, args);
    }
}
