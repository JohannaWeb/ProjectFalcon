package app.falcon.siv.service;

import app.falcon.core.domain.AiFact;
import app.falcon.siv.repository.AiFactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class IntelligenceVettingServiceTest {

    @Mock
    private AiFactRepository repository;

    @InjectMocks
    private IntelligenceVettingService vettingService;

    private static final String TARGET_DID = "did:plc:target";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void calculateBias_NoFacts_ReturnsNeutralBias() {
        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of());

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(1.0, bias, "No facts should result in a neutral bias of 1.0");
    }

    @Test
    void calculateBias_OnlyOldFacts_ReturnsNeutralBias() {
        AiFact oldFact = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(0.99)
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(oldFact));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(1.0, bias, "Facts older than 24 hours should be ignored");
    }

    @Test
    void calculateBias_RecentHighlight_IncreasesBias() {
        AiFact highlight = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(0.8) // + 0.1 * 0.8 = +0.08
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(highlight));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(1.08, bias, 0.001, "A recent highlight should increase the bias");
    }

    @Test
    void calculateBias_RecentWarning_DecreasesBias() {
        AiFact warning = AiFact.builder()
                .factType(AiFact.FactType.WARNING)
                .confidence(0.9) // - 0.2 * 0.9 = -0.18
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(warning));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(0.82, bias, 0.001, "A recent warning should decrease the bias");
    }

    @Test
    void calculateBias_TagsAndSummaries_AreNeutral() {
        AiFact tag = AiFact.builder()
                .factType(AiFact.FactType.TAG)
                .confidence(0.9)
                .createdAt(Instant.now())
                .build();
        AiFact summary = AiFact.builder()
                .factType(AiFact.FactType.SUMMARY)
                .confidence(0.9)
                .createdAt(Instant.now())
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(tag, summary));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(1.0, bias, "Tags and summaries should not affect bias");
    }

    @Test
    void calculateBias_ClampsToMaximum() {
        // We need enough highlights to exceed 1.5.
        // 1 highlight @ 1.0 confidence = +0.1. We need 6 of them to reach +0.6 (total
        // 1.6).
        AiFact highlight = AiFact.builder()
                .factType(AiFact.FactType.HIGHLIGHT)
                .confidence(1.0)
                .createdAt(Instant.now())
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(highlight, highlight, highlight, highlight, highlight, highlight));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(1.5, bias, "Bias should be clamped to a maximum of 1.5");
    }

    @Test
    void calculateBias_ClampsToMinimum() {
        // We need enough warnings to go below 0.5.
        // 1 warning @ 1.0 confidence = -0.2. We need 3 of them to reach -0.6 (total
        // 0.4).
        AiFact warning = AiFact.builder()
                .factType(AiFact.FactType.WARNING)
                .confidence(1.0)
                .createdAt(Instant.now())
                .build();

        when(repository.findTop20BySourceDidOrderByCreatedAtDesc(TARGET_DID))
                .thenReturn(List.of(warning, warning, warning));

        double bias = vettingService.calculateBias(TARGET_DID);

        assertEquals(0.5, bias, "Bias should be clamped to a minimum of 0.5");
    }
}
