package app.falcon.siv.ai;

import app.falcon.core.domain.AiFact;
import app.falcon.siv.repository.AiFactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AiContextServiceTest {

    @Mock
    private FalconAiClient aiClient;
    @Mock
    private AiFactRepository factRepository;

    private SovereignAgentService agentService;
    private AiContextService aiContextService;

    private static final String AGENT_DID = "did:plc:test-agent";
    private static final String TEST_DID = "did:plc:alice";
    // Valid tagging JSON response from the AI
    private static final String TAGGING_RESPONSE = "{\"tags\":[\"developer\",\"opensource\"],\"summary\":\"A post about open source development.\",\"factType\":\"TAG\",\"confidence\":0.9}";
    // Safe moderation response
    private static final String SAFE_RESPONSE = "{\"isHarmful\":false,\"reason\":\"\",\"confidence\":0.0}";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        agentService = new SovereignAgentService(AGENT_DID, "Test AI Agent");
        aiContextService = new AiContextService(aiClient, factRepository, agentService,
                50, 60, 5000);

        when(aiClient.complete(anyString(), anyString()))
                .thenReturn(Mono.just(TAGGING_RESPONSE)) // first call: tagging
                .thenReturn(Mono.just(SAFE_RESPONSE)); // second call: moderation
        when(factRepository.save(any(AiFact.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processPostPersistsTagsAndSummary() throws InterruptedException {
        aiContextService.processPost(TEST_DID, "I just shipped my first open source library!");

        // Virtual thread — give it a moment to complete
        Thread.sleep(500);

        ArgumentCaptor<AiFact> captor = ArgumentCaptor.forClass(AiFact.class);
        // Expect 3 saves: 2 tags + 1 summary
        verify(factRepository, atLeast(3)).save(captor.capture());

        boolean hasTag = captor.getAllValues().stream()
                .anyMatch(f -> f.getFactType() == AiFact.FactType.TAG && f.getContent().equals("developer"));
        boolean hasSummary = captor.getAllValues().stream()
                .anyMatch(f -> f.getFactType() == AiFact.FactType.SUMMARY);

        assertTrue(hasTag, "Expected TAG fact with 'developer'");
        assertTrue(hasSummary, "Expected SUMMARY fact");
    }

    @Test
    void agentDidIsStampedOnFacts() throws InterruptedException {
        aiContextService.processPost(TEST_DID, "Testing the Falcon AI agent DID stamp.");

        Thread.sleep(500);

        ArgumentCaptor<AiFact> captor = ArgumentCaptor.forClass(AiFact.class);
        verify(factRepository, atLeast(1)).save(captor.capture());

        assertTrue(captor.getAllValues().stream()
                .allMatch(f -> AGENT_DID.equals(f.getAgentDid())),
                "All AiFacts should be stamped with the agent DID");
    }

    @Test
    void rateLimitSuppressesSecondCallWithinWindow() throws InterruptedException {
        // Rate limit window is 60s — second call should NOT trigger AI
        aiContextService.processPost(TEST_DID, "First post");
        Thread.sleep(500);
        reset(aiClient); // clear invocation count after first post
        when(aiClient.complete(anyString(), anyString())).thenReturn(Mono.just(TAGGING_RESPONSE));

        aiContextService.processPost(TEST_DID, "Second post within 60s");
        Thread.sleep(500);

        // AI should NOT have been called for the second post (still in cooldown)
        verify(aiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void blankTextIsIgnored() throws InterruptedException {
        aiContextService.processPost(TEST_DID, "   ");
        aiContextService.processPost(TEST_DID, null);
        Thread.sleep(200);

        verify(aiClient, never()).complete(anyString(), anyString());
    }

    @Test
    void harmfulContentCreatesWarningFact() throws InterruptedException {
        when(aiClient.complete(anyString(), anyString()))
                .thenReturn(Mono.just(TAGGING_RESPONSE))
                .thenReturn(Mono.just("{\"isHarmful\":true,\"reason\":\"Explicit harassment.\",\"confidence\":0.92}"));

        aiContextService.processPost(TEST_DID, "This is a harmful message.");
        Thread.sleep(500);

        ArgumentCaptor<AiFact> captor = ArgumentCaptor.forClass(AiFact.class);
        verify(factRepository, atLeast(1)).save(captor.capture());

        boolean hasWarning = captor.getAllValues().stream()
                .anyMatch(f -> f.getFactType() == AiFact.FactType.WARNING);
        assertTrue(hasWarning, "Expected a WARNING AiFact for harmful content");
    }
}
