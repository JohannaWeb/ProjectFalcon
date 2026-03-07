package app.juntos.alpha.repository;

import app.juntos.alpha.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByServerId(Long serverId);
}
