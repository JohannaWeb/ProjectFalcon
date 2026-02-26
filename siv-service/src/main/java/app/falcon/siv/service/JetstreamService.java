package app.falcon.siv.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class JetstreamService {

    private final JetstreamHandler jetstreamHandler;
    private static final String JETSTREAM_URL = "wss://jetstream1.us-east.bsky.network/subscribe?wantedCollections=app.bsky.feed.post";

    @PostConstruct
    public void connect() {
        log.info("🚀 Initializing Jetstream connection to {}...", JETSTREAM_URL);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketConnectionManager manager = new WebSocketConnectionManager(client, jetstreamHandler, JETSTREAM_URL);
        manager.setAutoStartup(true);
        manager.start();
    }
}
