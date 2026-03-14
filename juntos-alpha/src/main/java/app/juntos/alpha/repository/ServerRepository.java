package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<Server, Long> {

    @Query("SELECT DISTINCT s FROM Server s LEFT JOIN FETCH s.channels WHERE s IN (SELECT m.server FROM Member m WHERE m.did = :did)")
    List<Server> findByMembersDid(@Param("did") String did);

    /** Eagerly fetches channels to avoid LazyInitializationException outside a transaction. */
    @Query("SELECT s FROM Server s LEFT JOIN FETCH s.channels WHERE s.id = :id")
    Optional<Server> findByIdWithChannels(@Param("id") Long id);
}
