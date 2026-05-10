package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Member;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class MemberRepository implements PanacheRepository<Member> {
    public Optional<Member> findByDidAndServerId(String did, Long serverId) {
        return find("did = ?1 and serverId = ?2", did, serverId).firstResultOptional();
    }

    public boolean existsByDidAndServerId(String did, Long serverId) {
        return find("did = ?1 and serverId = ?2", did, serverId).count() > 0;
    }
}
