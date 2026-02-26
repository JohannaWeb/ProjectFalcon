package app.falcon.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Table(name = "user_siv_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSivConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userDid;

    @Column(nullable = false)
    private String vesselType; // e.g., "github"

    @Column(nullable = false)
    private String encryptedToken;

    @Convert(converter = app.falcon.core.util.JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> config;
}
