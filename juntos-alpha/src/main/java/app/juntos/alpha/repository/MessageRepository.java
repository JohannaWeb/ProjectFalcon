package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId, Pageable pageable);
}
