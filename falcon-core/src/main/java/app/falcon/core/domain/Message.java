package app.falcon.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "author_did", nullable = false)
    private String authorDid;

    @Column(name = "author_handle")
    private String authorHandle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void createdAt() {
        if (createdAt == null)
            createdAt = Instant.now();
    }
}
