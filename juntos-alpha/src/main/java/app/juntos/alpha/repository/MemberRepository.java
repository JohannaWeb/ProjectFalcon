package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByDidAndServerId(String did, Long serverId);
    boolean existsByDidAndServerId(String did, Long serverId);
}
