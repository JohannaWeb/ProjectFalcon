package app.falcon.alpha.repository;

import app.falcon.alpha.domain.DidDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DidDocumentRepository extends JpaRepository<DidDocument, String> {
}
