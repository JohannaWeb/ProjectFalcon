package app.falcon.alpha.repository;

import app.falcon.alpha.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByDidAndServerId(String did, Long serverId);
    boolean existsByDidAndServerId(String did, Long serverId);
}
