package org.rooms.roombuddy.repository;

import org.rooms.roombuddy.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {
    
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);
    
    @Query("SELECT c FROM IssueComment c WHERE c.issue.id = :issueId AND c.isResolutionProposal = true ORDER BY c.createdAt DESC")
    List<IssueComment> findResolutionProposals(@Param("issueId") UUID issueId);
    
    @Query("SELECT COUNT(c) FROM IssueComment c WHERE c.issue.id = :issueId")
    long countByIssueId(@Param("issueId") UUID issueId);
}
