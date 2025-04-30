package com.professionalloan.management.controller;

import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.ApplicationStatus;
import com.professionalloan.management.service.LoanApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:5173")
public class LoanApplicationController {
    @Autowired
    private LoanApplicationService loanService;

    @PostMapping(value = "/apply", consumes = "multipart/form-data")
    public ResponseEntity<?> submitApplication(
            @RequestParam("name") String name,
            @RequestParam("profession") String profession,
            @RequestParam("purpose") String purpose,
            @RequestParam("loanAmount") BigDecimal loanAmount,
            @RequestParam("panCard") String panCard,
            @RequestParam("tenureInMonths") Integer tenureInMonths,
            @RequestParam("userId") Long userId,
            @RequestParam("pfAccountPdf") MultipartFile pfAccountPdf,
            @RequestParam("salarySlip") MultipartFile salarySlip
    ) throws IOException {
        if (pfAccountPdf.getSize() > 5 * 1024 * 1024 || salarySlip.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("PDF files must be under 5MB");
        }
        LoanApplication savedApplication = loanService.submitApplicationWithFiles(
                name, profession, purpose, loanAmount, panCard,
                tenureInMonths,
                userId, pfAccountPdf, salarySlip
        );
        return ResponseEntity.ok(savedApplication);
    }

    // - Admin can update status with comment ---
    @PutMapping("/update-status/{applicationId}")
    public ResponseEntity<?> updateLoanStatusWithComment(
            @PathVariable String applicationId,
            @RequestParam String status,
            @RequestParam(required = false) String comment) {
        LoanApplication updated = loanService.updateLoanStatusWithComment(
            applicationId, ApplicationStatus.valueOf(status.toUpperCase()), comment
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    public ResponseEntity<List<LoanApplication>> getAllApplications() {
        List<LoanApplication> allApplications = loanService.getAllApplications();
        return ResponseEntity.ok(allApplications);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanApplication>> getUserApplications(@PathVariable Long userId) {
        List<LoanApplication> userApplications = loanService.getApplicationsByUserId(userId);
        return ResponseEntity.ok(userApplications);
    }

    // --- List all documents for a user ---
    @GetMapping("/documents/user/{userId}")
    public ResponseEntity<List<DocumentInfo>> getUserDocuments(@PathVariable Long userId) {
        List<LoanApplication> applications = loanService.getApplicationsByUserId(userId);
        List<DocumentInfo> docs = new ArrayList<>();
        for (LoanApplication app : applications) {
            if (app.getPfAccountPdf() != null) {
                docs.add(new DocumentInfo(
                        app.getApplicationId(),
                        "PF Account Statement",
                        "pf_account_" + app.getApplicationId() + ".pdf",
                        "/api/loans/documents/download/" + app.getApplicationId() + "/pf"
                ));
            }
            if (app.getSalarySlip() != null) {
                docs.add(new DocumentInfo(
                        app.getApplicationId(),
                        "Salary Slip",
                        "salary_slip_" + app.getApplicationId() + ".pdf",
                        "/api/loans/documents/download/" + app.getApplicationId() + "/salary"
                ));
            }
        }
        return ResponseEntity.ok(docs);
    }

    // --- : List all documents for a specific application ---
    @GetMapping("/documents/application/{applicationId}")
    public ResponseEntity<List<DocumentInfo>> getApplicationDocuments(@PathVariable String applicationId) {
        LoanApplication app = loanService.getApplicationById(applicationId);
        if (app == null) {
            return ResponseEntity.notFound().build();
        }
        List<DocumentInfo> docs = new ArrayList<>();
        if (app.getPfAccountPdf() != null) {
            docs.add(new DocumentInfo(
                app.getApplicationId(),
                "PF Account Statement",
                "pf_account_" + app.getApplicationId() + ".pdf",
                "/api/loans/documents/download/" + app.getApplicationId() + "/pf"
            ));
        }
        if (app.getSalarySlip() != null) {
            docs.add(new DocumentInfo(
                app.getApplicationId(),
                "Salary Slip",
                "salary_slip_" + app.getApplicationId() + ".pdf",
                "/api/loans/documents/download/" + app.getApplicationId() + "/salary"
            ));
        }
        return ResponseEntity.ok(docs);
    }

    // --- Download a document for a given application ---
    @GetMapping("/documents/download/{applicationId}/{type}")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable String applicationId,
            @PathVariable String type) {
        LoanApplication app = loanService.getApplicationById(applicationId);
        byte[] fileData;
        String fileName;
        if ("pf".equals(type)) {
            fileData = app.getPfAccountPdf();
            fileName = "pf_account_" + applicationId + ".pdf";
        } else if ("salary".equals(type)) {
            fileData = app.getSalarySlip();
            fileName = "salary_slip_" + applicationId + ".pdf";
        } else {
            return ResponseEntity.badRequest().build();
        }
        if (fileData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(fileData);
    }

    // --- Inner class for document metadata ---
    public static class DocumentInfo {
        private String applicationId;
        private String documentType;
        private String fileName;
        private String downloadUrl;

        public DocumentInfo(String applicationId, String documentType, String fileName, String downloadUrl) {
            this.applicationId = applicationId;
            this.documentType = documentType;
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
        }

        public String getApplicationId() { return applicationId; }
        public String getDocumentType() { return documentType; }
        public String getFileName() { return fileName; }
        public String getDownloadUrl() { return downloadUrl; }
    }
}