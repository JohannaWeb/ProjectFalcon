package app.falcon.trust;

import app.falcon.domain.TrustRelation;
import app.falcon.repository.TrustRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class TrustGraphServiceTest {

    @Mock
    private TrustRelationRepository repository;

    private TrustGraphService service;

    private final String ME = "did:plc:me";
    private final String FRIEND = "did:plc:friend";
    private final String STRANGER = "did:plc:stranger";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TrustGraphService(repository);
    }

    @Test
    void testDirectTrust() {
        TrustRelation direct = TrustRelation.builder()
                .sourceDid(ME)
                .targetDid(FRIEND)
                .type(TrustRelation.TrustType.TRUST)
                .build();

        when(repository.findBySourceDidAndTargetDid(ME, FRIEND)).thenReturn(Optional.of(direct));

        double score = service.calculateTrustScore(ME, FRIEND);
        assertEquals(1.0, score, 0.01);
    }

    @Test
    void testTransitiveTrust() {
        // ME trusts FRIEND
        when(repository.findBySourceDidAndTargetDid(ME, STRANGER)).thenReturn(Optional.empty());

        TrustRelation meToFriend = TrustRelation.builder()
                .sourceDid(ME)
                .targetDid(FRIEND)
                .type(TrustRelation.TrustType.TRUST)
                .build();
        when(repository.findBySourceDidAndType(ME, TrustRelation.TrustType.TRUST)).thenReturn(List.of(meToFriend));

        // FRIEND trusts STRANGER
        TrustRelation friendToStranger = TrustRelation.builder()
                .sourceDid(FRIEND)
                .targetDid(STRANGER)
                .type(TrustRelation.TrustType.TRUST)
                .build();
        when(repository.findBySourceDidAndTargetDid(FRIEND, STRANGER)).thenReturn(Optional.of(friendToStranger));

        // Expected score: 1.0 (Friend's trust) * 0.5 (Decay) = 0.5
        double score = service.calculateTrustScore(ME, STRANGER);
        assertEquals(0.5, score, 0.01);
    }

    @Test
    void testNeutralScore() {
        when(repository.findBySourceDidAndTargetDid(ME, STRANGER)).thenReturn(Optional.empty());
        when(repository.findBySourceDidAndType(ME, TrustRelation.TrustType.TRUST)).thenReturn(List.of());

        double score = service.calculateTrustScore(ME, STRANGER);
        assertEquals(0.0, score, 0.01);
    }
}
