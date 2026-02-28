package app.falcon.siv.ai;

import app.falcon.core.domain.AiFact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AutonomousVoucherTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private SovereignAgentService agentService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AutonomousVoucher autonomousVoucher;

    private static final String TARGET_DID = "did:plc:target";
    private static final String AGENT_DID = "did:plc:agent";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(autonomousVoucher, "trustServiceUrl", "http://localhost:8081");

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("attestationId", "txn123")));

        when(agentService.getAgentDid()).thenReturn(AGENT_DID);
    }

    @Test
    void evaluateVouch_HighConfidenceHighlight_TriggersVouch() {
        AiFact fact = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(0.96)
                .sourceDid(TARGET_DID)
                .build();

        autonomousVoucher.evaluateVouch(fact);

        // Verify the entire WebClient chain up to subscribe
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        assertEquals("http://localhost:8081/api/trust/attest/" + TARGET_DID, uriCaptor.getValue());

        verify(requestBodySpec).header(eq("X-Falcon-Viewer-DID"), eq(AGENT_DID));
        verify(responseSpec).bodyToMono(Map.class);
    }

    @Test
    void evaluateVouch_LowConfidenceHighlight_DoesNotTriggerVouch() {
        AiFact fact = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(0.94) // Threshold is 0.95
                .sourceDid(TARGET_DID)
                .build();

        autonomousVoucher.evaluateVouch(fact);

        verify(webClientBuilder, never()).build();
    }

    @Test
    void evaluateVouch_HighConfidenceNonHighlight_DoesNotTriggerVouch() {
        AiFact fact = AiFact.builder()
                .factType(AiFact.FactType.SUMMARY)
                .confidence(0.99)
                .sourceDid(TARGET_DID)
                .build();

        autonomousVoucher.evaluateVouch(fact);

        verify(webClientBuilder, never()).build();
    }

    @Test
    void evaluateVouch_NullConfidence_DoesNotThrow() {
        AiFact fact = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(null)
                .sourceDid(TARGET_DID)
                .build();

        autonomousVoucher.evaluateVouch(fact);

        verify(webClientBuilder, never()).build();
    }
}
