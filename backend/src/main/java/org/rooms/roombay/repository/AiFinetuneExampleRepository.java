package org.rooms.roombay.repository;

import org.rooms.roombay.entity.AiFinetuneExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiFinetuneExampleRepository extends JpaRepository<AiFinetuneExample, UUID> {

    List<AiFinetuneExample> findByActiveTrueOrderByCreatedAtDesc();

    List<AiFinetuneExample> findByActiveTrueAndKind(AiFinetuneExample.Kind kind);

    long countByActiveTrue();

    long countByActiveTrueAndKind(AiFinetuneExample.Kind kind);

    @Query("SELECT e.kind AS kind, COUNT(e) AS total FROM AiFinetuneExample e WHERE e.active = true GROUP BY e.kind")
    List<KindCount> countActiveByKind();

    interface KindCount {
        AiFinetuneExample.Kind getKind();
        long getTotal();
    }
}
