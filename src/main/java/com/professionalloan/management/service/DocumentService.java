package com.professionalloan.management.service;

import com.professionalloan.management.model.Document;
import com.professionalloan.management.model.User;
import com.professionalloan.management.repository.DocumentRepository;
import com.professionalloan.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public Document saveDocument(MultipartFile file, Long userId, String documentType) {
        try {
            // Validate inputs
            if (file == null || file.isEmpty()) {
                logger.error("File is null or empty");
                throw new IllegalArgumentException("File cannot be null or empty");
            }
            if (userId == null) {
                logger.error("User ID is null");
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (documentType == null || documentType.trim().isEmpty()) {
                logger.error("Document type is null or empty");
                throw new IllegalArgumentException("Document type cannot be null or empty");
            }

            // Get user
            logger.info("Fetching user with ID: {}", userId);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, String.valueOf(userId));
            logger.info("Creating upload directory if not exists: {}", uploadPath);
            try {
                Files.createDirectories(uploadPath);
            } catch (Exception e) {
                logger.error("Failed to create upload directory: {}", uploadPath, e);
                throw new RuntimeException("Could not create upload directory: " + e.getMessage());
            }

            // Verify directory is writable
            if (!Files.isWritable(uploadPath)) {
                logger.error("Upload directory is not writable: {}", uploadPath);
                throw new RuntimeException("Upload directory is not writable");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                logger.error("Original filename is null or empty");
                throw new IllegalArgumentException("Invalid file name");
            }
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file to filesystem
            Path filePath = uploadPath.resolve(newFilename);
            logger.info("Saving file to: {}", filePath);
            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                logger.error("Failed to save file to filesystem: {}", filePath, e);
                throw new RuntimeException("Could not store file: " + e.getMessage());
            }

            // Create document record
            logger.info("Creating document record for file: {}", originalFilename);
            Document document = new Document(
                user,
                originalFilename,
                documentType,
                filePath.toString(),
                LocalDateTime.now(),
                file.getSize(),
                file.getContentType()
            );

            // Save document to database
            Document savedDocument = documentRepository.save(document);
            logger.info("Document saved successfully with ID: {}", savedDocument.getDocumentId());
            return savedDocument;
        } catch (Exception e) {
            logger.error("Error in saveDocument: {}", e.getMessage(), e);
            throw new RuntimeException("Could not store file. Error: " + e.getMessage());
        }
    }

    public List<Document> getUserDocuments(Long userId) {
        try {
            logger.info("Fetching documents for user ID: {}", userId);
            return documentRepository.findByUser_Id(userId);
        } catch (Exception e) {
            logger.error("Error fetching documents for user ID: {}", userId, e);
            throw new RuntimeException("Could not fetch documents: " + e.getMessage());
        }
    }

    public Resource loadDocument(Long documentId) {
        try {
            logger.info("Loading document with ID: {}", documentId);
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    logger.error("Document not found with ID: {}", documentId);
                    return new RuntimeException("Document not found");
                });

            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.info("Document loaded successfully: {}", filePath);
                return resource;
            } else {
                logger.error("Could not read file: {}", filePath);
                throw new RuntimeException("Could not read file");
            }
        } catch (Exception e) {
            logger.error("Error loading document ID: {}", documentId, e);
            throw new RuntimeException("Error loading document: " + e.getMessage());
        }
    }

    public void deleteDocument(Long documentId) {
        try {
            logger.info("Deleting document with ID: {}", documentId);
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    logger.error("Document not found with ID: {}", documentId);
                    return new RuntimeException("Document not found");
                });

            // Delete file from filesystem
            Path filePath = Paths.get(document.getFilePath());
            try {
                Files.deleteIfExists(filePath);
                logger.info("File deleted from filesystem: {}", filePath);
            } catch (Exception e) {
                logger.error("Failed to delete file from filesystem: {}", filePath, e);
            }

            // Delete database record
            documentRepository.delete(document);
            logger.info("Document deleted successfully: {}", documentId);
        } catch (Exception e) {
            logger.error("Error deleting document ID: {}", documentId, e);
            throw new RuntimeException("Error deleting document: " + e.getMessage());
        }
    }

    public List<Document> getUserDocumentsByType(Long userId, String documentType) {
        try {
            logger.info("Fetching documents for user ID: {} and type: {}", userId, documentType);
            return documentRepository.findByUser_IdAndFileType(userId, documentType);
        } catch (Exception e) {
            logger.error("Error fetching documents for user ID: {} and type: {}", userId, documentType, e);
            throw new RuntimeException("Could not fetch documents: " + e.getMessage());
        }
    }

    public void verifyDocument(Long documentId) {
        try {
            logger.info("Verifying document with ID: {}", documentId);
            Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    logger.error("Document not found with ID: {}", documentId);
                    return new RuntimeException("Document not found");
                });
            document.setVerified(true);
            documentRepository.save(document);
            logger.info("Document verified successfully: {}", documentId);
        } catch (Exception e) {
            logger.error("Error verifying document ID: {}", documentId, e);
            throw new RuntimeException("Could not verify document: " + e.getMessage());
        }
    }
}