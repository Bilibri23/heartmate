package org.rooms.roombay.repository;

import org.rooms.roombay.entity.RealtorVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RealtorVerificationDocumentRepository extends JpaRepository<RealtorVerificationDocument, UUID> {

    List<RealtorVerificationDocument> findByRealtorIdOrderByCreatedAtDesc(UUID realtorId);
}
