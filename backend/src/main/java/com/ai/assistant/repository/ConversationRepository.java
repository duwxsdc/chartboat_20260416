package com.ai.assistant.repository;

import com.ai.assistant.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserId(Long userId);
    Optional<Conversation> findByUserIdAndConversationId(Long userId, String conversationId);
    void deleteByUserIdAndConversationId(Long userId, String conversationId);
}