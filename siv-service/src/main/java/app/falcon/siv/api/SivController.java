package app.falcon.siv.api;

import app.falcon.siv.service.SivIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/siv")
@RequiredArgsConstructor
public class SivController {

    private final SivIntelligenceService intelligenceService;

    @GetMapping("/intelligence")
    public Map<String, Object> getIntelligence(
            @RequestParam String userDid) {
        return intelligenceService.fetchIntelligence(userDid);
    }
}
