package app.falcon.alpha.repository;

import app.falcon.alpha.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByServerId(Long serverId);
}
