package app.falcon.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An AI-generated fact about a channel event — a tag, summary, highlight, or
 * warning.
 * Stamped with the agent's DID so the source is always auditable.
 */
@Entity
@Table(name = "ai_facts", indexes = {
        @Index(name = "idx_ai_facts_channel", columnList = "channel_id"),
        @Index(name = "idx_ai_facts_agent_did", columnList = "agent_did")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The channel (or DID-as-proxy-channel on the firehose) this fact is about.
     */
    @Column(name = "channel_id", nullable = false)
    private String channelId;

    /**
     * The DID of the AT Protocol post author that triggered this analysis.
     */
    @Column(name = "source_did", nullable = false)
    private String sourceDid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FactType factType;

    /**
     * The AI-generated content: a tag name, a summary sentence, a warning message,
     * etc.
     */
    @Column(nullable = false, length = 2000)
    private String content;

    /**
     * Confidence score [0.0, 1.0] as self-reported by the model.
     */
    @Column
    private Double confidence;

    /**
     * The DID of the Falcon AI agent that generated this fact.
     * This is the "sovereign" signature — every AI decision is attributed to a DID.
     */
    @Column(name = "agent_did", nullable = false)
    private String agentDid;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum FactType {
        TAG, // e.g. "developer", "atproto", "open-source"
        SUMMARY, // rolling channel summary
        HIGHLIGHT, // positive signal worth surfacing
        WARNING // potentially harmful content
    }
}
