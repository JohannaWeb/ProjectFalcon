package app.falcon.siv.api;

import app.falcon.siv.ai.SovereignAgentService;
import app.falcon.siv.ai.SovereignAgentService.AgentMemoryState;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final SovereignAgentService agentService;

    @GetMapping("/state")
    public Map<String, String> getState() {
        return Map.of("state", agentService.getMemoryState().name());
    }

    @PostMapping("/state")
    public Map<String, String> setState(@RequestBody Map<String, String> body) {
        String raw = body.get("state");
        AgentMemoryState state = AgentMemoryState.valueOf(raw.toUpperCase());
        agentService.setMemoryState(state);
        return Map.of("state", state.name());
    }
}
