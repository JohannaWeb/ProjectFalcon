package app.falcon.alpha.repository;

import app.falcon.alpha.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT cp.conversation FROM ConversationParticipant cp WHERE cp.did = :did ORDER BY cp.conversation.createdAt DESC")
    List<Conversation> findByParticipantDid(@Param("did") String did);
}
