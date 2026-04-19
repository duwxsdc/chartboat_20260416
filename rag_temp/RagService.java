package com.ai.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();

    public void addDocument(Document document) {
        try {
            documentStore.put(document.getId(), document);
            log.info("Document added: {}", document.getId());
        } catch (Exception e) {
            log.error("Failed to add document: {}", e.getMessage());
        }
    }

    public void addDocuments(List<Document> documents) {
        try {
            for (Document document : documents) {
                documentStore.put(document.getId(), document);
            }
            log.info("Added {} documents", documents.size());
        } catch (Exception e) {
            log.error("Failed to add documents: {}", e.getMessage());
        }
    }

    public List<Document> search(String query, int topK) {
        try {
            // 简单的关键词匹配，实际项目中应使用更复杂的检索算法
            List<Document> results = new ArrayList<>();
            for (Document doc : documentStore.values()) {
                if (doc.getContent().toLowerCase().contains(query.toLowerCase())) {
                    results.add(doc);
                    if (results.size() >= topK) {
                        break;
                    }
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public String getRelevantContext(String query, int topK) {
        List<Document> documents = search(query, topK);
        StringBuilder context = new StringBuilder();
        for (Document doc : documents) {
            context.append(doc.getContent()).append("\n\n");
        }
        return context.toString();
    }

    public void deleteDocument(String id) {
        try {
            documentStore.remove(id);
            log.info("Document deleted: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete document: {}", e.getMessage());
        }
    }

    public void clearAll() {
        try {
            documentStore.clear();
            log.info("All documents cleared");
        } catch (Exception e) {
            log.error("Failed to clear documents: {}", e.getMessage());
        }
    }

    // 简单的Document类，替代Spring AI的Document
    public static class Document {
        private String id;
        private String content;
        private Map<String, Object> metadata;

        public Document(String id, String content, Map<String, Object> metadata) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}