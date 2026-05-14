package app.juntos.alpha.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "google_users")
@NoArgsConstructor
public class GoogleUser extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String googleSub;

    @Column(nullable = false)
    public String email;

    /** did:plc:... created on plc.directory for this user */
    @Column(unique = true, nullable = false)
    public String did;

    /** PKCS8 DER of the P-256 private key, base64-encoded */
    @Column(nullable = false, length = 512)
    public String signingKeyPkcs8;

    public static GoogleUser findBySub(String sub) {
        return find("googleSub", sub).firstResult();
    }
}
