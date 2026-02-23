package app.falcon.repository;

import app.falcon.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findByMembersDid(String did);
}
