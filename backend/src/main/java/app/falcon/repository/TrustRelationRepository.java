package app.falcon.repository;

import app.falcon.domain.TrustRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrustRelationRepository extends JpaRepository<TrustRelation, Long> {
    List<TrustRelation> findBySourceDid(String sourceDid);

    List<TrustRelation> findByTargetDid(String targetDid);

    Optional<TrustRelation> findBySourceDidAndTargetDid(String sourceDid, String targetDid);

    List<TrustRelation> findBySourceDidAndType(String sourceDid, TrustRelation.TrustType type);
}
