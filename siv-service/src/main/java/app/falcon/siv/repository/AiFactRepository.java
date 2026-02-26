package app.falcon.siv.repository;

import app.falcon.core.domain.AiFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiFactRepository extends JpaRepository<AiFact, Long> {

    List<AiFact> findTop20ByChannelIdOrderByCreatedAtDesc(String channelId);

    List<AiFact> findTop20BySourceDidOrderByCreatedAtDesc(String sourceDid);

    List<AiFact> findByAgentDidAndFactType(String agentDid, AiFact.FactType factType);
}
