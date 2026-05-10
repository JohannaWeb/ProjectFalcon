package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Channel;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ChannelRepository implements PanacheRepository<Channel> {
    public List<Channel> findByServerId(Long serverId) {
        return find("serverId", serverId).list();
    }

    public boolean existsById(Long id) {
        return count("id", id) > 0;
    }
}
