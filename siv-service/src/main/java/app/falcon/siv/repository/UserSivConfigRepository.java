package app.falcon.siv.repository;

import app.falcon.core.domain.UserSivConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSivConfigRepository extends JpaRepository<UserSivConfig, Long> {
    List<UserSivConfig> findByUserDid(String userDid);

    Optional<UserSivConfig> findByUserDidAndVesselType(String userDid, String vesselType);
}
