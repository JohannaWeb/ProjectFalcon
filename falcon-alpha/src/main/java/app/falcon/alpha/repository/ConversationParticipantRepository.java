package app.falcon.alpha.repository;

import app.falcon.alpha.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    List<ConversationParticipant> findByConversationId(Long conversationId);

    List<ConversationParticipant> findByDid(String did);
}
