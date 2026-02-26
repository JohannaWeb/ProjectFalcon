package app.falcon.siv.pulse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SivPulseRepository extends JpaRepository<SivPulse, Long> {
    List<SivPulse> findByActorDidOrderByTimestampDesc(String actorDid);

    List<SivPulse> findByVesselTypeOrderByTimestampDesc(String vesselType);
}
