package com.ai.assistant.memory;

import com.ai.assistant.config.ContextManagementConfig;
import com.ai.assistant.util.CompressionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TieredChatMemory implements ChatMemory {

    private static final String HOT_PREFIX = "chat:hot:";
    private static final String WARM_PREFIX = "chat:warm:";
    private static final String COLD_PREFIX = "chat:cold:";
    private static final String SUMMARY_PREFIX = "chat:summary:";
    private static final String META_PREFIX = "chat:meta:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ContextManagementConfig config;
    private final CompressionUtil compressionUtil;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private ConversationBackupService backupService;

    private final Map<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> localCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public void setBackupService(ConversationBackupService backupService) {
        this.backupService = backupService;
    }

    @Autowired
    public TieredChatMemory(RedisTemplate<String, Object> redisTemplate,
                           ContextManagementConfig config,
                           CompressionUtil compressionUtil) {
        this.redisTemplate = redisTemplate;
        this.config = config;
        this.compressionUtil = compressionUtil;
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(2);

        startAutoCleanup();
        startMonitor();
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<Message> existingMessages = getOrLoadMessages(conversationId);
            List<Message> updatedMessages = new ArrayList<>(existingMessages);
            updatedMessages.addAll(messages);

            int hotSize = config.getHotTierSize();
            int warmSize = config.getWarmTierSize();

            if (updatedMessages.size() <= hotSize) {
                saveToHotTier(conversationId, updatedMessages);
                deleteFromLowerTiers(conversationId);
            } else if (updatedMessages.size() <= warmSize) {
                List<Message> hotMessages = updatedMessages.subList(updatedMessages.size() - hotSize, updatedMessages.size());
                List<Message> warmMessages = updatedMessages.subList(0, updatedMessages.size() - hotSize);

                saveToHotTier(conversationId, hotMessages);
                saveToWarmTier(conversationId, warmMessages);
                deleteFromColdTier(conversationId);
            } else {
                List<Message> hotMessages = updatedMessages.subList(updatedMessages.size() - hotSize, updatedMessages.size());
                List<Message> warmMessages = updatedMessages.subList(updatedMessages.size() - warmSize, updatedMessages.size() - hotSize);
                List<Message> coldMessages = updatedMessages.subList(0, updatedMessages.size() - warmSize);

                saveToHotTier(conversationId, hotMessages);
                saveToWarmTier(conversationId, warmMessages);
                saveToColdTier(conversationId, coldMessages);
            }

            updateMetadata(conversationId, updatedMessages.size());
            localCache.put(conversationId, updatedMessages);

            if (backupService != null && updatedMessages.size() % 20 == 0) {
                backupService.createBackup(conversationId, updatedMessages);
            }

            if (config.getSummary().isEnabled() && updatedMessages.size() >= config.getSummary().getMaxMessagesBeforeSummary()) {
                scheduleSummaryGeneration(conversationId, updatedMessages);
            }

            log.debug("Added {} messages to conversation {}. Total: {}", messages.size(), conversationId, updatedMessages.size());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<Message> allMessages = getOrLoadMessages(conversationId);
            updateLastAccessTime(conversationId);

            return allMessages;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear(String conversationId) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(conversationId, k -> new ReentrantLock());
        lock.lock();
        try {
            if (backupService != null) {
                List<Message> currentMessages = getOrLoadMessages(conversationId);
                if (!currentMessages.isEmpty()) {
                    backupService.createBackup(conversationId, currentMessages);
                }
            }
            deleteFromAllTiers(conversationId);
            localCache.remove(conversationId);
            log.info("Cleared conversation: {}", conversationId);
        } finally {
            lock.unlock();
        }
    }

    public boolean rollback(String conversationId) {
        if (backupService == null) {
            log.warn("Backup service not available for rollback");
            return false;
        }
        return backupService.rollback(conversationId, this);
    }

    private List<Message> getOrLoadMessages(String conversationId) {
        if (localCache.containsKey(conversationId)) {
            return new ArrayList<>(localCache.get(conversationId));
        }

        List<Message> hotMessages = loadFromHotTier(conversationId);
        if (hotMessages == null || hotMessages.isEmpty()) {
            hotMessages = loadFromWarmTier(conversationId);
        }

        List<Message> warmMessages = loadFromWarmTier(conversationId);
        List<Message> coldMessages = loadFromColdTier(conversationId);
        List<Message> summaryMessages = loadSummary(conversationId);

        List<Message> allMessages = new ArrayList<>();
        if (summaryMessages != null && !summaryMessages.isEmpty()) {
            allMessages.addAll(summaryMessages);
        }
        if (coldMessages != null && !coldMessages.isEmpty()) {
            allMessages.addAll(coldMessages);
        }
        if (warmMessages != null && !warmMessages.isEmpty()) {
            allMessages.addAll(warmMessages);
        }
        if (hotMessages != null && !hotMessages.isEmpty()) {
            allMessages.addAll(hotMessages);
        }

        List<Message> result = new ArrayList<>(allMessages);
        localCache.put(conversationId, result);
        return result;
    }

    private void saveToHotTier(String conversationId, List<Message> messages) {
        try {
            String key = HOT_PREFIX + conversationId;
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize hot tier messages: {}", e.getMessage());
        }
    }

    private void saveToWarmTier(String conversationId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        try {
            String key = WARM_PREFIX + conversationId;
            String json = objectMapper.writeValueAsString(messages);
            if (config.getCompression().isEnabled()) {
                String compressed = compressionUtil.compressToBase64(json);
                if (compressed.length() < json.length()) {
                    redisTemplate.opsForValue().set(key + ":compressed", compressed, DEFAULT_TTL.multipliedBy(7));
                    return;
                }
            }
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL.multipliedBy(7));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize warm tier messages: {}", e.getMessage());
        }
    }

    private void saveToColdTier(String conversationId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        try {
            String key = COLD_PREFIX + conversationId;
            String json = objectMapper.writeValueAsString(messages);
            String compressed = compressionUtil.compressToBase64(json);
            redisTemplate.opsForValue().set(key + ":compressed", compressed, DEFAULT_TTL.multipliedBy(30));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cold tier messages: {}", e.getMessage());
        }
    }

    private List<Message> loadFromHotTier(String conversationId) {
        String key = HOT_PREFIX + conversationId;
        Object data = redisTemplate.opsForValue().get(key);
        return deserializeMessages(data);
    }

    private List<Message> loadFromWarmTier(String conversationId) {
        String key = WARM_PREFIX + conversationId;
        Object data = redisTemplate.opsForValue().get(key);
        if (data == null) {
            Object compressed = redisTemplate.opsForValue().get(key + ":compressed");
            if (compressed != null) {
                String decompressed = compressionUtil.decompressFromBase64(compressed.toString());
                if (!decompressed.isEmpty()) {
                    return deserializeMessages(decompressed);
                }
            }
            return null;
        }
        return deserializeMessages(data);
    }

    private List<Message> loadFromColdTier(String conversationId) {
        String key = COLD_PREFIX + conversationId;
        Object compressed = redisTemplate.opsForValue().get(key + ":compressed");
        if (compressed != null) {
            String decompressed = compressionUtil.decompressFromBase64(compressed.toString());
            if (!decompressed.isEmpty()) {
                return deserializeMessages(decompressed);
            }
        }
        return null;
    }

    private void deleteFromLowerTiers(String conversationId) {
        redisTemplate.delete(WARM_PREFIX + conversationId);
        redisTemplate.delete(WARM_PREFIX + conversationId + ":compressed");
        redisTemplate.delete(COLD_PREFIX + conversationId);
        redisTemplate.delete(COLD_PREFIX + conversationId + ":compressed");
    }

    private void deleteFromColdTier(String conversationId) {
        redisTemplate.delete(COLD_PREFIX + conversationId);
        redisTemplate.delete(COLD_PREFIX + conversationId + ":compressed");
    }

    private void deleteFromAllTiers(String conversationId) {
        redisTemplate.delete(HOT_PREFIX + conversationId);
        redisTemplate.delete(WARM_PREFIX + conversationId);
        redisTemplate.delete(WARM_PREFIX + conversationId + ":compressed");
        redisTemplate.delete(COLD_PREFIX + conversationId);
        redisTemplate.delete(COLD_PREFIX + conversationId + ":compressed");
        redisTemplate.delete(SUMMARY_PREFIX + conversationId);
        redisTemplate.delete(META_PREFIX + conversationId);
    }

    private List<Message> deserializeMessages(Object data) {
        if (data == null) {
            return new ArrayList<>();
        }
        try {
            String json = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
            return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize messages: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void updateMetadata(String conversationId, int messageCount) {
        String key = META_PREFIX + conversationId;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("messageCount", messageCount);
        metadata.put("lastAccessTime", Instant.now().toString());
        metadata.put("createdTime", Instant.now().toString());
        redisTemplate.opsForHash().putAll(key, metadata);
        redisTemplate.expire(key, DEFAULT_TTL.multipliedBy(30));
    }

    private void updateLastAccessTime(String conversationId) {
        String key = META_PREFIX + conversationId;
        redisTemplate.opsForHash().put(key, "lastAccessTime", Instant.now().toString());
    }

    private void saveSummary(String conversationId, List<Message> messages) {
        String combinedText = messages.stream()
                .map(m -> m.getMessageType() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        String key = SUMMARY_PREFIX + conversationId;
        redisTemplate.opsForValue().set(key, combinedText, DEFAULT_TTL.multipliedBy(7));
    }

    private List<Message> loadSummary(String conversationId) {
        return null;
    }

    private void scheduleSummaryGeneration(String conversationId, List<Message> messages) {
        scheduler.submit(() -> {
            try {
                String summaryText = generateSummary(messages);
                if (summaryText != null && !summaryText.isEmpty()) {
                    String key = SUMMARY_PREFIX + conversationId;
                    redisTemplate.opsForValue().set(key, summaryText, DEFAULT_TTL.multipliedBy(7));
                    log.info("Generated summary for conversation {}: {} chars", conversationId, summaryText.length());
                }
            } catch (Exception e) {
                log.error("Failed to generate summary for conversation {}: {}", conversationId, e.getMessage());
            }
        });
    }

    private String generateSummary(List<Message> messages) {
        if (messages.size() < 10) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[对话摘要]\n");

        int userCount = 0;
        int assistantCount = 0;
        Set<String> topics = new HashSet<>();

        for (Message msg : messages) {
            if (msg instanceof UserMessage) {
                userCount++;
            } else if (msg instanceof AssistantMessage) {
                assistantCount++;
            }
        }

        sb.append(String.format("- 共 %d 条用户消息, %d 条助手回复\n", userCount, assistantCount));

        return sb.toString();
    }

    private void startAutoCleanup() {
        if (!config.getAutoCleanup().isEnabled()) {
            return;
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                performAutoCleanup();
            } catch (Exception e) {
                log.error("Auto cleanup failed: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void performAutoCleanup() {
        int maxIdleHours = config.getAutoCleanup().getMaxIdleHours();
        Instant threshold = Instant.now().minusSeconds(maxIdleHours * 3600L);

        Set<String> hotKeys = redisTemplate.keys(HOT_PREFIX + "*");
        if (hotKeys == null) {
            return;
        }

        int cleanedCount = 0;
        for (String key : hotKeys) {
            String conversationId = key.substring(HOT_PREFIX.length());
            String metaKey = META_PREFIX + conversationId;
            Object lastAccess = redisTemplate.opsForHash().get(metaKey, "lastAccessTime");

            if (lastAccess != null) {
                Instant lastAccessTime = Instant.parse(lastAccess.toString());
                if (lastAccessTime.isBefore(threshold)) {
                    clear(conversationId);
                    cleanedCount++;
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("Auto cleanup: removed {} idle conversations", cleanedCount);
        }
    }

    private void startMonitor() {
        if (!config.getMonitor().isEnabled()) {
            return;
        }

        int intervalSeconds = config.getMonitor().getLogIntervalSeconds();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performMonitoring();
            } catch (Exception e) {
                log.error("Monitor failed: {}", e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void performMonitoring() {
        Set<String> hotKeys = redisTemplate.keys(HOT_PREFIX + "*");
        Set<String> warmKeys = redisTemplate.keys(WARM_PREFIX + "*");
        Set<String> coldKeys = redisTemplate.keys(COLD_PREFIX + "*");

        int hotCount = hotKeys != null ? hotKeys.size() : 0;
        int warmCount = warmKeys != null ? warmKeys.size() : 0;
        int coldCount = coldKeys != null ? coldKeys.size() : 0;

        log.info("Context Monitor - Hot: {} conversations, Warm: {}, Cold: {}, LocalCache: {}",
                hotCount, warmCount, coldCount, localCache.size());
    }

    public Map<String, Object> getConversationStats(String conversationId) {
        Map<String, Object> stats = new HashMap<>();

        List<Message> messages = getOrLoadMessages(conversationId);
        stats.put("totalMessages", messages.size());
        stats.put("hotTierSize", Math.min(config.getHotTierSize(), messages.size()));

        int warmMessages = Math.max(0, messages.size() - config.getHotTierSize());
        stats.put("warmTierSize", Math.min(config.getWarmTierSize(), warmMessages));

        int coldMessages = Math.max(0, messages.size() - config.getWarmTierSize());
        stats.put("coldTierSize", coldMessages);

        String key = META_PREFIX + conversationId;
        Object lastAccess = redisTemplate.opsForHash().get(key, "lastAccessTime");
        stats.put("lastAccessTime", lastAccess != null ? lastAccess.toString() : "unknown");

        return stats;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}