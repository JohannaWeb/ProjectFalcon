package app.falcon.core.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(columnNames = { "server_id", "did" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String did;

    @Column(name = "handle")
    private String handle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    public enum MemberRole {
        OWNER, MODERATOR, MEMBER
    }
}
