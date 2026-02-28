package app.falcon.trust.service;

import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.repository.TrustRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrustGraphServiceTest {

        @Mock
        private TrustRelationRepository repository;

        @Mock
        private WebClient.Builder webClientBuilder;

        @InjectMocks
        private TrustGraphService trustGraphService;

        @BeforeEach
        void setUp() {
                MockitoAnnotations.openMocks(this);
        }

        // ─── Self-trust ─────────────────────────────────────────────────────────────

        @Test
        void selfAlwaysReturnsOne() {
                assertEquals(1.0, trustGraphService.calculateTrustScore("did:alice", "did:alice"));
        }

        @Test
        void nullViewerAlwaysReturnsOne() {
                assertEquals(1.0, trustGraphService.calculateTrustScore(null, "did:alice"));
        }

        // ─── Direct relations ────────────────────────────────────────────────────────

        @Test
        void directTrustReturnsOneFull() {
                TrustRelation rel = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:bob")
                                .type(TrustRelation.TrustType.TRUST).build();
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.of(rel));

                assertEquals(1.0, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        @Test
        void directDistrustReturnsNegativeScore() {
                TrustRelation rel = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:bob")
                                .type(TrustRelation.TrustType.DISTRUST).build();
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.of(rel));

                assertEquals(-0.8, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        @Test
        void directBlockReturnsMinusOne() {
                TrustRelation rel = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:bob")
                                .type(TrustRelation.TrustType.BLOCK).build();
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.of(rel));

                assertEquals(-1.0, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        @Test
        void directMuteReturnsNegativeLow() {
                TrustRelation rel = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:bob")
                                .type(TrustRelation.TrustType.MUTE).build();
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.of(rel));

                assertEquals(-0.3, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        // ─── Bridge (transitive) trust ───────────────────────────────────────────────

        @Test
        void noBridgesReturnsZero() {
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.empty());
                when(repository.findBySourceDidAndType("did:alice", TrustRelation.TrustType.TRUST))
                                .thenReturn(List.of());

                assertEquals(0.0, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        @Test
        void singleBridgeTrustProducesHalfScore() {
                // alice trusts carol, carol trusts bob => score = 1.0 * 0.5 / 1 = 0.5
                TrustRelation aliceToCarol = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:carol")
                                .type(TrustRelation.TrustType.TRUST).build();
                TrustRelation carolToBob = TrustRelation.builder()
                                .sourceDid("did:carol").targetDid("did:bob")
                                .type(TrustRelation.TrustType.TRUST).build();

                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.empty());
                when(repository.findBySourceDidAndType("did:alice", TrustRelation.TrustType.TRUST))
                                .thenReturn(List.of(aliceToCarol));
                when(repository.findBySourceDidAndTargetDid("did:carol", "did:bob"))
                                .thenReturn(Optional.of(carolToBob));

                assertEquals(0.5, trustGraphService.calculateTrustScore("did:alice", "did:bob"));
        }

        @Test
        void twoBridgesTrustAveragedAndClamped() {
                // alice trusts carol and dave; both trust bob => score clamped to [-1, 1]
                TrustRelation aliceToCarol = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:carol")
                                .type(TrustRelation.TrustType.TRUST).build();
                TrustRelation aliceToDave = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:dave")
                                .type(TrustRelation.TrustType.TRUST).build();
                TrustRelation carolToBob = TrustRelation.builder()
                                .sourceDid("did:carol").targetDid("did:bob")
                                .type(TrustRelation.TrustType.TRUST).build();
                TrustRelation daveToBob = TrustRelation.builder()
                                .sourceDid("did:dave").targetDid("did:bob")
                                .type(TrustRelation.TrustType.BLOCK).build();

                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.empty());
                when(repository.findBySourceDidAndType("did:alice", TrustRelation.TrustType.TRUST))
                                .thenReturn(List.of(aliceToCarol, aliceToDave));
                when(repository.findBySourceDidAndTargetDid("did:carol", "did:bob"))
                                .thenReturn(Optional.of(carolToBob));
                when(repository.findBySourceDidAndTargetDid("did:dave", "did:bob"))
                                .thenReturn(Optional.of(daveToBob));

                // carol -> TRUST (score 1.0 * 0.5 = 0.5), dave -> BLOCK (score -1.0 * 0.5 =
                // -0.5)
                // aggregate = 0.5 + (-0.5) = 0.0 / 2 bridges = 0.0
                double score = trustGraphService.calculateTrustScore("did:alice", "did:bob");
                assertEquals(0.0, score, 0.0001);
        }

        // ─── addRelation ─────────────────────────────────────────────────────────────

        @Test
        void addRelationCreatesNewIfNotExists() {
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.empty());

                trustGraphService.addRelation("did:alice", "did:bob", TrustRelation.TrustType.TRUST);

                verify(repository, times(1)).save(any(TrustRelation.class));
        }

        @Test
        void addRelationUpdatesExisting() {
                TrustRelation existing = TrustRelation.builder()
                                .sourceDid("did:alice").targetDid("did:bob")
                                .type(TrustRelation.TrustType.TRUST).build();
                when(repository.findBySourceDidAndTargetDid("did:alice", "did:bob"))
                                .thenReturn(Optional.of(existing));

                trustGraphService.addRelation("did:alice", "did:bob", TrustRelation.TrustType.BLOCK);

                verify(repository, times(1)).save(existing);
                assertEquals(TrustRelation.TrustType.BLOCK, existing.getType());
        }
}
