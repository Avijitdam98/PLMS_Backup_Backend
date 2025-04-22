package com.professionalloan.management.controller;

import com.professionalloan.management.model.Document;
import com.professionalloan.management.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("documentType") String documentType
    ) {
        try {
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File must be under 5MB");
            }
            Document savedDocument = documentService.saveDocument(file, userId, documentType);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded successfully");
            response.put("documentId", savedDocument.getDocumentId());
            response.put("fileName", savedDocument.getFileName());
            response.put("fileType", savedDocument.getFileType());
            response.put("uploadDate", savedDocument.getUploadDate());
            response.put("fileSize", savedDocument.getFileSize());
            response.put("mimeType", savedDocument.getMimeType());
            response.put("isVerified", savedDocument.isVerified());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload document: " + e.getMessage());
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Document>> getUserDocuments(@PathVariable Long userId) {
        try {
            List<Document> documents = documentService.getUserDocuments(userId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
    
    @GetMapping("/download/{documentId}")
    public ResponseEntity<?> downloadDocument(@PathVariable Long documentId) {
        try {
            Resource file = documentService.loadDocument(documentId);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Could not download file");
        }
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Could not delete document");
        }
    }

    @GetMapping("/type/{userId}/{documentType}")
    public ResponseEntity<List<Document>> getUserDocumentsByType(
            @PathVariable Long userId,
            @PathVariable String documentType) {
        try {
            List<Document> documents = documentService.getUserDocumentsByType(userId, documentType);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @PutMapping("/verify/{documentId}")
    public ResponseEntity<?> verifyDocument(@PathVariable Long documentId) {
        try {
            documentService.verifyDocument(documentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Could not verify document");
        }
    }
}