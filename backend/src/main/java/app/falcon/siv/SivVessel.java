package app.falcon.siv;

import java.util.List;
import java.util.Map;

/**
 * Common interface for Sovereign Integration Vessels (SIV).
 */
public interface SivVessel {
    String getVesselType();
    List<Map<String, Object>> fetchActivity(String token, Map<String, Object> config);
}
