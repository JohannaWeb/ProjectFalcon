package app.falcon.siv.api;

import app.falcon.siv.api.dto.IntelligenceResponse;
import app.falcon.siv.service.SivIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/siv")
@RequiredArgsConstructor
public class SivController {

    private final SivIntelligenceService intelligenceService;

    @GetMapping("/intelligence")
    public IntelligenceResponse getIntelligence(
            @RequestParam String userDid) {
        return intelligenceService.fetchIntelligence(userDid);
    }
}
