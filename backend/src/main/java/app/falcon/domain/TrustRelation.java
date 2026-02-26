package app.falcon.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "trust_relations", uniqueConstraints = @UniqueConstraint(columnNames = { "source_did", "target_did" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_did", nullable = false)
    private String sourceDid;

    @Column(name = "target_did", nullable = false)
    private String targetDid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TrustType type;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Column
    private Double weight;

    public enum TrustType {
        TRUST, DISTRUST, BLOCK, MUTE
    }
}
