package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import java.util.List;

public interface SivVessel {
    String getType();

    List<?> fetchActivity(UserSivConfig config);
}
