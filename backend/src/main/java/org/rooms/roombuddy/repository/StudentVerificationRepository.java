package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.StudentVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentVerificationRepository extends JpaRepository<StudentVerification, UUID> {
    Optional<StudentVerification> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
    long countByStatus(StudentVerification.Status status);
    long countByUserId(UUID userId);
}

