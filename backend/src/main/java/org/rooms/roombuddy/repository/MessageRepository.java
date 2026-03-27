package org.rooms.roombay.repository;

import org.rooms.roombay.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    /**
     * Get conversation between two users (ordered oldest first for chat display)
     */
    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2 
           OR m.sender.id = :userId2 AND m.receiver.id = :userId1)
        AND ((m.sender.id = :userId1 AND m.isDeletedBySender = false)
           OR (m.receiver.id = :userId1 AND m.isDeletedByReceiver = false))
        ORDER BY m.createdAt ASC
    """)
    Page<Message> findConversation(
        @Param("userId1") UUID userId1, 
        @Param("userId2") UUID userId2,
        Pageable pageable
    );
    
    /**
     * Get all conversations for a user (last message from each conversation)
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.id IN (
            SELECT MAX(m2.id) FROM Message m2
            WHERE (m2.sender.id = :userId OR m2.receiver.id = :userId)
            AND ((m2.sender.id = :userId AND m2.isDeletedBySender = false)
               OR (m2.receiver.id = :userId AND m2.isDeletedByReceiver = false))
            GROUP BY 
                CASE 
                    WHEN m2.sender.id = :userId THEN m2.receiver.id
                    ELSE m2.sender.id
                END
        )
        ORDER BY m.createdAt DESC
    """)
    List<Message> findLastMessagesForUser(@Param("userId") UUID userId);
    
    /**
     * Count unread messages from a specific user
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.sender.id = :senderId 
        AND m.receiver.id = :receiverId
        AND m.isRead = false
        AND m.isDeletedByReceiver = false
    """)
    Long countUnreadMessages(
        @Param("senderId") UUID senderId, 
        @Param("receiverId") UUID receiverId
    );
    
    /**
     * Count total unread messages for a user
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.receiver.id = :userId
        AND m.isRead = false
        AND m.isDeletedByReceiver = false
    """)
    Long countTotalUnreadMessages(@Param("userId") UUID userId);
    
    /**
     * Mark all messages as read
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP
        WHERE m.sender.id = :senderId 
        AND m.receiver.id = :receiverId
        AND m.isRead = false
    """)
    void markMessagesAsRead(
        @Param("senderId") UUID senderId, 
        @Param("receiverId") UUID receiverId
    );
    
    /**
     * Search messages in a conversation
     */
    @Query("""
        SELECT m FROM Message m
        WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2 
           OR m.sender.id = :userId2 AND m.receiver.id = :userId1)
        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        AND ((m.sender.id = :userId1 AND m.isDeletedBySender = false)
           OR (m.receiver.id = :userId1 AND m.isDeletedByReceiver = false))
        ORDER BY m.createdAt DESC
    """)
    Page<Message> searchInConversation(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * Count distinct conversations
     */
    @Query("""
        SELECT COUNT(DISTINCT 
            CASE 
                WHEN m.sender.id < m.receiver.id THEN CONCAT(m.sender.id, '-', m.receiver.id)
                ELSE CONCAT(m.receiver.id, '-', m.sender.id)
            END
        ) FROM Message m
    """)
    long countConversations();
}
