package com.ai.assistant.controller;

import com.ai.assistant.service.AuthService;
import com.ai.assistant.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class RagController {

    private final RagService ragService;
    private final AuthService authService;

    @PostMapping("/documents")
    public ResponseEntity<?> addDocument(@RequestParam("file") MultipartFile file) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        try {
            String content = new String(file.getBytes());
            RagService.Document document = new RagService.Document(
                    UUID.randomUUID().toString(),
                    content,
                    Map.of(
                            "filename", file.getOriginalFilename(),
                            "contentType", file.getContentType(),
                            "userId", user.getId().toString()
                    )
            );
            ragService.addDocument(document);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Document added successfully",
                    "documentId", document.getId()
            ));
        } catch (IOException e) {
            log.error("Failed to read file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to read file"
            ));
        }
    }

    @PostMapping("/documents/batch")
    public ResponseEntity<?> addDocuments(@RequestParam("files") MultipartFile[] files) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        try {
            List<RagService.Document> documents = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                String content = new String(file.getBytes());
                RagService.Document document = new RagService.Document(
                        UUID.randomUUID().toString(),
                        content,
                        Map.of(
                                "filename", file.getOriginalFilename(),
                                "contentType", file.getContentType(),
                                "userId", user.getId().toString()
                        )
                );
                documents.add(document);
            }
            ragService.addDocuments(documents);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Documents added successfully",
                    "count", documents.size()
            ));
        } catch (IOException e) {
            log.error("Failed to read files: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to read files"
            ));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        ragService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Document deleted successfully"
        ));
    }

    @DeleteMapping("/documents")
    public ResponseEntity<?> clearAllDocuments() {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        ragService.clearAll();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All documents cleared"
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, Object> request) {
        var user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? (Integer) request.get("topK") : 3;

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
        }

        var documents = ragService.search(query, topK);
        var results = documents.stream()
                .map(doc -> Map.of(
                        "id", doc.getId(),
                        "title", doc.getMetadata().get("filename"),
                        "content", doc.getContent(),
                        "metadata", doc.getMetadata()
                ))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "results", results
        ));
    }
}