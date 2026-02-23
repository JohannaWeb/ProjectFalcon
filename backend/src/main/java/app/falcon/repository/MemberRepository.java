package app.falcon.repository;

import app.falcon.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByServerIdAndDid(Long serverId, String did);

    boolean existsByServerIdAndDid(Long serverId, String did);
}
