package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LinearVessel implements SivVessel {
    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "linear";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        // Simplified for migration, original logic can be restored
        return List.of();
    }
}
