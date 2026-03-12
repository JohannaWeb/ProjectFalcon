package app.falcon.alpha.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "conversation_participants")
@Data
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(nullable = false)
    private String did;

    @Column(nullable = false)
    private String handle;
}
