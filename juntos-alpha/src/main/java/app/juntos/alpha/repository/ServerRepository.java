package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Server;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ServerRepository implements PanacheRepository<Server> {

    @Inject
    EntityManager em;

    public List<Server> findByMembersDid(String did) {
        return em.createQuery(
                "SELECT DISTINCT s FROM Server s LEFT JOIN FETCH s.channels WHERE s IN (SELECT m.server FROM Member m WHERE m.did = :did)",
                Server.class)
                .setParameter("did", did)
                .getResultList();
    }

    public Optional<Server> findByIdWithChannels(Long id) {
        List<Server> results = em.createQuery(
                "SELECT s FROM Server s LEFT JOIN FETCH s.channels WHERE s.id = :id",
                Server.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
