package com.ai.assistant.service;

import com.ai.assistant.model.Conversation;
import com.ai.assistant.model.Message;
import com.ai.assistant.repository.ConversationRepository;
import com.ai.assistant.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public Conversation createConversation(Long userId, String conversationId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setConversationId(conversationId);
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getConversationsByUserId(Long userId) {
        return conversationRepository.findByUserId(userId);
    }

    public Optional<Conversation> getConversationByUserIdAndConversationId(Long userId, String conversationId) {
        return conversationRepository.findByUserIdAndConversationId(userId, conversationId);
    }

    public void deleteConversation(Long userId, String conversationId) {
        conversationRepository.deleteByUserIdAndConversationId(userId, conversationId);
    }

    public Message saveMessage(Long conversationId, String role, String content) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        return messageRepository.save(message);
    }

    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationId(conversationId);
    }

    public void deleteMessagesByConversationId(Long conversationId) {
        messageRepository.deleteByConversationId(conversationId);
    }

    public Conversation updateConversationTitle(Long userId, String conversationId, String title) {
        Optional<Conversation> optionalConversation = conversationRepository.findByUserIdAndConversationId(userId, conversationId);
        if (optionalConversation.isPresent()) {
            Conversation conversation = optionalConversation.get();
            conversation.setTitle(title);
            return conversationRepository.save(conversation);
        }
        return null;
    }
}