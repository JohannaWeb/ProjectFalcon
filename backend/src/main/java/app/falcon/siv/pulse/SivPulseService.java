package app.falcon.siv.pulse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SivPulseService {
    private final SivPulseRepository repository;

    @Transactional
    public SivPulse recordPulse(String vesselType, String pulseType, String actorDid, Map<String, Object> payload) {
        SivPulse pulse = SivPulse.builder()
                .vesselType(vesselType)
                .pulseType(pulseType)
                .actorDid(actorDid)
                .payload(payload)
                .build();
        return repository.save(pulse);
    }

    public List<SivPulse> getPulsesForUser(String actorDid) {
        return repository.findByActorDidOrderByTimestampDesc(actorDid);
    }
}
