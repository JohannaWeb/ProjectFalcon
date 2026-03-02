package app.falcon.alpha.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "channels")
@Data
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
}
