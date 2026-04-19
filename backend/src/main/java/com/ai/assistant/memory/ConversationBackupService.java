package com.ai.assistant.memory;

import com.ai.assistant.config.ContextManagementConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConversationBackupService {

    private static final String BACKUP_PREFIX = "chat:backup:";
    private static final String BACKUP_INDEX_PREFIX = "chat:backup:index:";
    private static final Duration BACKUP_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ContextManagementConfig config;
    private final ObjectMapper objectMapper;

    private final Map<String, Instant> lastBackupTime = new ConcurrentHashMap<>();

    public ConversationBackupService(RedisTemplate<String, Object> redisTemplate,
                                     ContextManagementConfig config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public boolean createBackup(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
        try {
            String backupKey = BACKUP_PREFIX + conversationId + ":" + Instant.now().toEpochMilli();
            String json = objectMapper.writeValueAsString(messages);

            redisTemplate.opsForValue().set(backupKey, json, BACKUP_TTL);

            addToBackupIndex(conversationId, backupKey);

            lastBackupTime.put(conversationId, Instant.now());
            log.info("Created backup for conversation {}: {} messages", conversationId, messages.size());
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to create backup for conversation {}: {}", conversationId, e.getMessage());
            return false;
        }
    }

    public List<org.springframework.ai.chat.messages.Message> getLatestBackup(String conversationId) {
        List<String> backupKeys = getBackupKeys(conversationId);
        if (backupKeys == null || backupKeys.isEmpty()) {
            return null;
        }

        String latestKey = backupKeys.get(backupKeys.size() - 1);
        Object data = redisTemplate.opsForValue().get(latestKey);

        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readValue(data.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, org.springframework.ai.chat.messages.Message.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize backup for conversation {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    public List<org.springframework.ai.chat.messages.Message> restoreFromBackup(String conversationId, long timestamp) {
        String backupKey = BACKUP_PREFIX + conversationId + ":" + timestamp;
        Object data = redisTemplate.opsForValue().get(backupKey);

        if (data == null) {
            log.warn("Backup not found for conversation {} at timestamp {}", conversationId, timestamp);
            return null;
        }

        try {
            return objectMapper.readValue(data.toString(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, org.springframework.ai.chat.messages.Message.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to restore backup for conversation {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    public boolean rollback(String conversationId, org.springframework.ai.chat.memory.ChatMemory chatMemory) {
        List<org.springframework.ai.chat.messages.Message> backup = getLatestBackup(conversationId);
        if (backup == null || backup.isEmpty()) {
            log.warn("No backup found for rollback: {}", conversationId);
            return false;
        }

        try {
            chatMemory.clear(conversationId);
            chatMemory.add(conversationId, backup);
            log.info("Successfully rolled back conversation {} with {} messages", conversationId, backup.size());
            return true;
        } catch (Exception e) {
            log.error("Rollback failed for conversation {}: {}", conversationId, e.getMessage());
            return false;
        }
    }

    public boolean deleteBackup(String conversationId, long timestamp) {
        String backupKey = BACKUP_PREFIX + conversationId + ":" + timestamp;
        Boolean result = redisTemplate.delete(backupKey);
        if (result != null && result) {
            removeFromBackupIndex(conversationId, backupKey);
            log.info("Deleted backup for conversation {} at {}", conversationId, timestamp);
        }
        return result != null && result;
    }

    public int deleteAllBackups(String conversationId) {
        List<String> backupKeys = getBackupKeys(conversationId);
        if (backupKeys == null || backupKeys.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String key : backupKeys) {
            Boolean result = redisTemplate.delete(key);
            if (result != null && result) {
                count++;
            }
        }

        redisTemplate.delete(BACKUP_INDEX_PREFIX + conversationId);
        lastBackupTime.remove(conversationId);

        log.info("Deleted {} backups for conversation {}", count, conversationId);
        return count;
    }

    public List<Long> getBackupTimestamps(String conversationId) {
        List<String> keys = getBackupKeys(conversationId);
        if (keys == null) {
            return new ArrayList<>();
        }

        List<Long> timestamps = new ArrayList<>();
        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                if (parts.length >= 4) {
                    timestamps.add(Long.parseLong(parts[3]));
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid backup key format: {}", key);
            }
        }

        return timestamps;
    }

    private void addToBackupIndex(String conversationId, String backupKey) {
        String indexKey = BACKUP_INDEX_PREFIX + conversationId;
        redisTemplate.opsForList().rightPush(indexKey, backupKey);
        redisTemplate.expire(indexKey, BACKUP_TTL);
    }

    private void removeFromBackupIndex(String conversationId, String backupKey) {
        String indexKey = BACKUP_INDEX_PREFIX + conversationId;
        redisTemplate.opsForList().remove(indexKey, 1, backupKey);
    }

    private List<String> getBackupKeys(String conversationId) {
        String indexKey = BACKUP_INDEX_PREFIX + conversationId;
        List<Object> keys = redisTemplate.opsForList().range(indexKey, 0, -1);
        if (keys == null) {
            return new ArrayList<>();
        }
        return keys.stream().map(Object::toString).toList();
    }

    public Map<String, Object> getBackupStats(String conversationId) {
        Map<String, Object> stats = new HashMap<>();
        List<Long> timestamps = getBackupTimestamps(conversationId);

        stats.put("backupCount", timestamps.size());
        stats.put("timestamps", timestamps);
        stats.put("lastBackupTime", lastBackupTime.getOrDefault(conversationId, Instant.MIN).toString());

        if (!timestamps.isEmpty()) {
            stats.put("oldestBackup", timestamps.get(0));
            stats.put("newestBackup", timestamps.get(timestamps.size() - 1));
        }

        return stats;
    }
}