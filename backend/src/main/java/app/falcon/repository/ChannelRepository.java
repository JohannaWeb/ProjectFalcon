package app.falcon.repository;

import app.falcon.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByServerIdOrderById(Long serverId);
}
