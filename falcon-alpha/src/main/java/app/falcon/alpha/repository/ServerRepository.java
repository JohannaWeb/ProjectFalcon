package app.falcon.alpha.repository;

import app.falcon.alpha.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {
    List<Server> findByMembersDid(String did);
}
