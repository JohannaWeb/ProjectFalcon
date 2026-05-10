package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Message;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {
    public List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId) {
        return find("channelId", channelId).stream().sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())).toList();
    }
}
