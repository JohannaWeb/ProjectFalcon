package app.falcon.repository;

import app.falcon.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);
}
