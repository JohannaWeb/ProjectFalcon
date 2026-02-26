package app.falcon.trust.repository;

import app.falcon.core.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByDidAndServerId(String did, Long serverId);
}
