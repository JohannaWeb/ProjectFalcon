package app.falcon.alpha.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "did_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DidDocument {

    @Id
    private String did;

    @Column(columnDefinition = "TEXT")
    private String documentJson;

    private LocalDateTime lastUpdated;
}
