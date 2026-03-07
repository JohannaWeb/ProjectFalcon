package app.juntos.alpha.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "did"}))
@Data
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String did;

    @Column(nullable = false)
    private String handle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
}
