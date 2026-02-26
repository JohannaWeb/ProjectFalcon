package app.falcon.api;

import app.falcon.domain.UserSivConfig;
import app.falcon.repository.UserSivConfigRepository;
import app.falcon.siv.SivVessel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/siv")
@RequiredArgsConstructor
public class SivController {

    private final UserSivConfigRepository repository;
    private final List<SivVessel> vessels;

    @GetMapping("/configs")
    public ResponseEntity<List<UserSivConfig>> getConfigs(HttpServletRequest request) {
        String userDid = (String) request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        return ResponseEntity.ok(repository.findByUserDid(userDid));
    }

    @PostMapping("/configs")
    public ResponseEntity<UserSivConfig> saveConfig(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String userDid = (String) request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        String vesselType = (String) body.get("vesselType");
        String token = (String) body.get("token");
        Map<String, Object> config = (Map<String, Object>) body.get("config");

        Optional<UserSivConfig> existing = repository.findByUserDidAndVesselType(userDid, vesselType);
        UserSivConfig sivConfig = existing.orElse(new UserSivConfig());

        sivConfig.setUserDid(userDid);
        sivConfig.setVesselType(vesselType);
        sivConfig.setEncryptedToken(token); // In a real app, encrypt this
        sivConfig.setConfig(config);

        return ResponseEntity.ok(repository.save(sivConfig));
    }

    @GetMapping("/activity/{vesselType}")
    public ResponseEntity<List<Map<String, Object>>> getActivity(
            HttpServletRequest request,
            @PathVariable String vesselType) {

        String userDid = (String) request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        Optional<UserSivConfig> configOpt = repository.findByUserDidAndVesselType(userDid, vesselType);

        if (configOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserSivConfig config = configOpt.get();
        SivVessel vessel = vessels.stream()
                .filter(v -> v.getVesselType().equals(vesselType))
                .findFirst()
                .orElse(null);

        if (vessel == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> activity = vessel.fetchActivity(config.getEncryptedToken(), config.getConfig());
        return ResponseEntity.ok(activity);
    }
}
