package app.falcon.siv.pulse;

import app.falcon.domain.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "siv_pulses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SivPulse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vesselType; // e.g., "github", "linear"

    @Column(nullable = false)
    private String pulseType; // e.g., "COMMIT", "ISSUE_CREATED"

    @Column(nullable = false)
    private String actorDid; // The Bluesky DID associated

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> payload;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
