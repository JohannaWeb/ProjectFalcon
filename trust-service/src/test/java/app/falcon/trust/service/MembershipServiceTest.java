package app.falcon.trust.service;

import app.falcon.core.domain.Channel;
import app.falcon.core.domain.Member;
import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.repository.ChannelRepository;
import app.falcon.trust.repository.MemberRepository;
import app.falcon.trust.repository.TrustRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipServiceTest {

    private static final String AUTHORITY_DID = "did:plc:authority";

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private TrustRelationRepository trustRelationRepository;

    // Use explicit constructor so @Value-injected authorityDid is provided
    // correctly
    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        membershipService = new MembershipService(
                memberRepository, channelRepository, trustRelationRepository, AUTHORITY_DID);
    }

    @Test
    void testVerifyAccess_OpenChannel() {
        Channel channel = Channel.builder().id(1L).requiredTier(null).build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        boolean result = membershipService.verifyAccess("did:user", 1L, 1L);
        assertTrue(result);
    }

    @Test
    void testVerifyAccess_Gated_Pro_Success() {
        Channel channel = Channel.builder().id(1L).requiredTier("PRO").build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        // Authority trusts user with 0.9 weight
        TrustRelation relation = TrustRelation.builder()
                .sourceDid(AUTHORITY_DID)
                .targetDid("did:user")
                .type(TrustRelation.TrustType.TRUST)
                .weight(0.9)
                .build();
        when(trustRelationRepository.findBySourceDidAndTargetDid(AUTHORITY_DID, "did:user"))
                .thenReturn(Optional.of(relation));
        when(memberRepository.findByDidAndServerId("did:user", 1L)).thenReturn(Optional.empty());

        boolean result = membershipService.verifyAccess("did:user", 1L, 1L);
        assertTrue(result);
    }

    @Test
    void testVerifyAccess_Gated_Pro_Failure() {
        Channel channel = Channel.builder().id(1L).requiredTier("PRO").build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        when(trustRelationRepository.findBySourceDidAndTargetDid(AUTHORITY_DID, "did:user"))
                .thenReturn(Optional.empty());
        when(memberRepository.findByDidAndServerId("did:user", 1L)).thenReturn(Optional.empty());

        boolean result = membershipService.verifyAccess("did:user", 1L, 1L);
        assertFalse(result);
    }

    @Test
    void testVerifyAccess_ExplicitTier() {
        Channel channel = Channel.builder().id(1L).requiredTier("ELITE").build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        Member member = Member.builder().did("did:user").membershipTier("ELITE").build();
        when(memberRepository.findByDidAndServerId("did:user", 1L)).thenReturn(Optional.of(member));

        boolean result = membershipService.verifyAccess("did:user", 1L, 1L);
        assertTrue(result);
    }
}
