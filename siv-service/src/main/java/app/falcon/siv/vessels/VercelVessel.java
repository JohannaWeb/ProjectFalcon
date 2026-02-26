package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VercelVessel implements SivVessel {
    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "vercel";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        return List.of();
    }
}
