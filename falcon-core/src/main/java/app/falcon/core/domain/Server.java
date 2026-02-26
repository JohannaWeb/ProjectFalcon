package app.falcon.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_did", nullable = false)
    private String ownerDid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Channel> channels = new ArrayList<>();

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Member> members = new ArrayList<>();

    @PrePersist
    void createdAt() {
        if (createdAt == null)
            createdAt = Instant.now();
    }
}
