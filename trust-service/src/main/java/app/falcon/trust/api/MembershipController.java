package app.falcon.trust.api;

import app.falcon.trust.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trust/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/verify")
    public boolean verify(@RequestParam String userDid,
            @RequestParam Long serverId,
            @RequestParam Long channelId) {
        return membershipService.verifyAccess(userDid, serverId, channelId);
    }
}
