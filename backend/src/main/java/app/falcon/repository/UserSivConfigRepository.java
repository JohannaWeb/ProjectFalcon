package app.falcon.repository;

import app.falcon.domain.UserSivConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSivConfigRepository extends JpaRepository<UserSivConfig, Long> {
    List<UserSivConfig> findByUserDid(String userDid);

    Optional<UserSivConfig> findByUserDidAndVesselType(String userDid, String vesselType);
}
