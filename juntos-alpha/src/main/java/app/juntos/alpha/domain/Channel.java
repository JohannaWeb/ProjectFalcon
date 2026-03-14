package app.juntos.alpha.domain;

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

    /** AT Protocol URI: at://did/app.juntos.channel/rkey — set after successful PDS write. */
    @Column(unique = true)
    private String atUri;

    /** The TID rkey portion of the AT URI, for direct record lookup. */
    @Column
    private String atRkey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
}
