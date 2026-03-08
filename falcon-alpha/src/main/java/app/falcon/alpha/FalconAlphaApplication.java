package app.falcon.alpha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FalconAlphaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FalconAlphaApplication.class, args);
    }
}
