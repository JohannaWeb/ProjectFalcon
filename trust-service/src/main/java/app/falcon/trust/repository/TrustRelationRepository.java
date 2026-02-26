package app.falcon.trust.repository;

import app.falcon.core.domain.TrustRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustRelationRepository extends JpaRepository<TrustRelation, Long> {
    List<TrustRelation> findBySourceDid(String sourceDid);

    List<TrustRelation> findByTargetDid(String targetDid);

    Optional<TrustRelation> findBySourceDidAndTargetDid(String sourceDid, String targetDid);

    List<TrustRelation> findBySourceDidAndType(String sourceDid, TrustRelation.TrustType type);
}
