package app.falcon;

import app.falcon.api.AtprotoAuthFilter;
import app.falcon.api.ServerController;
import app.falcon.atproto.AtprotoException;
import app.falcon.atproto.XrpcClient;
import app.falcon.domain.Channel;
import app.falcon.domain.Member;
import app.falcon.domain.Server;
import app.falcon.repository.ChannelRepository;
import app.falcon.repository.MemberRepository;
import app.falcon.repository.ServerRepository;
import app.falcon.service.IdentityService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackendSecurityIntegrationTest {

    @Mock
    private XrpcClient xrpcClient;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private IdentityService identityService;

    @InjectMocks
    private ServerController serverController;

    @Test
    void authFilterRejectsMissingBearerToken() throws ServletException, IOException {
        AtprotoAuthFilter filter = new AtprotoAuthFilter(xrpcClient);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing bearer token");
    }

    @Test
    void authFilterDerivesIdentityFromTokenSession() throws ServletException, IOException {
        AtprotoAuthFilter filter = new AtprotoAuthFilter(xrpcClient);
        when(xrpcClient.get(eq("com.atproto.server.getSession"), any(), eq("valid-token")))
                .thenReturn(Map.of("did", "did:plc:alice", "handle", "alice.bsky.social"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/servers");
        request.addHeader("Authorization", "Bearer valid-token");
        request.addHeader("X-User-Did", "did:plc:spoofed");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID)).isEqualTo("did:plc:alice");
        assertThat(request.getAttribute(AtprotoAuthFilter.ATTR_USER_HANDLE)).isEqualTo("alice.bsky.social");
    }

    @Test
    void createServerUsesAuthenticatedDid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AtprotoAuthFilter.ATTR_USER_DID, "did:plc:alice");
        request.setAttribute(AtprotoAuthFilter.ATTR_USER_HANDLE, "alice.bsky.social");
        request.addHeader("X-User-Did", "did:plc:spoofed");

        Server savedServer = Server.builder().id(99L).name("Falcon Core").ownerDid("did:plc:alice").build();
        Channel savedChannel = Channel.builder().id(7L).name("general").server(savedServer).build();

        when(serverRepository.save(any(Server.class))).thenReturn(savedServer);
        when(channelRepository.save(any(Channel.class))).thenReturn(savedChannel);

        ResponseEntity<Map<String, Object>> response = serverController.createServer(request, Map.of("name", "Falcon Core"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("ownerDid")).isEqualTo("did:plc:alice");

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getDid()).isEqualTo("did:plc:alice");
    }

    @Test
    void inviteFailsWhenHandleCannotResolve() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AtprotoAuthFilter.ATTR_USER_DID, "did:plc:alice");

        Server server = Server.builder().id(1L).name("Falcon").ownerDid("did:plc:alice").build();

        when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
        when(identityService.resolveHandle("missing.bsky.social")).thenThrow(new AtprotoException(400, "not found"));

        ResponseEntity<?> response = serverController.inviteByHandle(1L, request, Map.of("handle", "missing.bsky.social"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void listServerReturnsForbiddenForNonMembers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AtprotoAuthFilter.ATTR_USER_DID, "did:plc:bob");

        when(memberRepository.existsByServerIdAndDid(anyLong(), eq("did:plc:bob"))).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = serverController.getServer(42L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
