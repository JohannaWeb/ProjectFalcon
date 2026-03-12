package app.falcon.alpha.api;

import app.falcon.alpha.auth.AtprotoAuthFilter;
import app.falcon.alpha.domain.Conversation;
import app.falcon.alpha.domain.ConversationMessage;
import app.falcon.alpha.domain.ConversationParticipant;
import app.falcon.alpha.repository.ConversationMessageRepository;
import app.falcon.alpha.repository.ConversationParticipantRepository;
import app.falcon.alpha.repository.ConversationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/xrpc")
@RequiredArgsConstructor
public class ConvoController {

    private final ConversationRepository convoRepo;
    private final ConversationParticipantRepository participantRepo;
    private final ConversationMessageRepository messageRepo;

    @GetMapping("/app.falcon.convo.list")
    public Map<String, Object> listConvos(HttpServletRequest req) {
        String viewerDid = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        List<Map<String, Object>> convos = convoRepo.findByParticipantDid(viewerDid).stream()
                .map(this::toConvoSummary)
                .toList();
        return Map.of("convos", convos);
    }

    @GetMapping("/app.falcon.convo.get")
    public ResponseEntity<Map<String, Object>> getConvo(@RequestParam Long convoId, HttpServletRequest req) {
        String viewerDid = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        Conversation convo = convoRepo.findById(convoId).orElse(null);
        if (convo == null)
            return ResponseEntity.notFound().build();

        if (!isParticipant(convo, viewerDid)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(toConvoSummary(convo));
    }

    @GetMapping("/app.falcon.convo.getMessages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @RequestParam Long convoId,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest req) {

        String viewerDid = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        Conversation convo = convoRepo.findById(convoId).orElse(null);
        if (convo == null)
            return ResponseEntity.notFound().build();

        if (!isParticipant(convo, viewerDid)) {
            return ResponseEntity.status(403).build();
        }

        List<Map<String, Object>> messages = messageRepo
                .findByConversationIdOrderByCreatedAtAsc(convoId, PageRequest.of(0, limit))
                .stream().map(this::toMessageSummary).toList();

        return ResponseEntity.ok(Map.of("messages", messages));
    }

    @PostMapping("/app.falcon.convo.sendMessage")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {

        String viewerDid = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        String content = (String) body.get("content");
        Number convoIdNum = (Number) body.get("convoId");

        Conversation convo;
        if (convoIdNum == null) {
            List<String> members = (List<String>) body.get("members");
            if (members == null || members.isEmpty())
                return ResponseEntity.badRequest().build();

            convo = new Conversation();
            convo = convoRepo.save(convo);

            List<String> allParticipants = new ArrayList<>(members);
            if (!allParticipants.contains(viewerDid))
                allParticipants.add(viewerDid);

            for (String did : allParticipants) {
                ConversationParticipant p = new ConversationParticipant();
                p.setConversation(convo);
                p.setDid(did);
                p.setHandle(did);
                participantRepo.save(p);
            }
            convo = convoRepo.findById(convo.getId()).orElseThrow();
        } else {
            convo = convoRepo.findById(convoIdNum.longValue()).orElse(null);
            if (convo == null)
                return ResponseEntity.notFound().build();
            if (!isParticipant(convo, viewerDid))
                return ResponseEntity.status(403).build();
        }

        ConversationMessage msg = new ConversationMessage();
        msg.setConversation(convo);
        msg.setContent(content);
        msg.setAuthorDid(viewerDid);
        msg.setAuthorHandle(viewerDid);
        ConversationMessage saved = messageRepo.save(msg);

        return ResponseEntity.ok(toMessageSummary(saved));
    }

    private boolean isParticipant(Conversation convo, String did) {
        return convo.getParticipants().stream().anyMatch(p -> p.getDid().equals(did));
    }

    private Map<String, Object> toConvoSummary(Conversation c) {
        return Map.of(
                "id", c.getId(),
                "participants", c.getParticipants().stream().map(ConversationParticipant::getDid).toList(),
                "createdAt", c.getCreatedAt().toString());
    }

    private Map<String, Object> toMessageSummary(ConversationMessage m) {
        return Map.of(
                "id", m.getId(),
                "content", m.getContent(),
                "authorDid", m.getAuthorDid(),
                "authorHandle", m.getAuthorHandle(),
                "createdAt", m.getCreatedAt().toString());
    }
}
