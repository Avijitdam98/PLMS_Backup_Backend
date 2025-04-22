package com.professionalloan.management.service;

import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.User;
import com.professionalloan.management.model.ApplicationStatus;
import com.professionalloan.management.repository.LoanApplicationRepository;
import com.professionalloan.management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoanApplicationService {
    @Autowired
    private LoanApplicationRepository loanRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DocumentService documentService;

    // Minimum credit score for approval
    private static final int MINIMUM_CREDIT_SCORE = 600;

    @Transactional
    public LoanApplication submitApplicationWithFiles(
            String name,
            String profession,
            String purpose,
            BigDecimal loanAmount,
            String panCard,
            Integer tenureInMonths, // <-- Added tenureInMonths parameter
            Long userId,
            MultipartFile pfAccountPdf,
            MultipartFile salarySlip
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Deterministically generate credit score from PAN
            int creditScore = fetchCreditScoreByPan(panCard);

            LoanApplication application = new LoanApplication();
            application.setApplicationId(UUID.randomUUID().toString());
            application.setName(name);
            application.setProfession(profession);
            application.setPurpose(purpose);
            application.setLoanAmount(loanAmount);
            application.setCreditScore(creditScore);
            application.setPanCard(panCard);
            application.setTenureInMonths(tenureInMonths); // <-- Set tenure here
            application.setUser(user);

            // Optionally store PDFs as bytes in LoanApplication (legacy)
            application.setPfAccountPdf(pfAccountPdf != null && !pfAccountPdf.isEmpty() ? pfAccountPdf.getBytes() : null);
            application.setSalarySlip(salarySlip != null && !salarySlip.isEmpty() ? salarySlip.getBytes() : null);

            // Credit assessment logic
            if (creditScore < MINIMUM_CREDIT_SCORE) {
                application.setStatus(ApplicationStatus.REJECTED);
                notificationService.notifyLoanStatus(userId, application.getApplicationId(), ApplicationStatus.REJECTED);
            } else {
                application.setStatus(ApplicationStatus.PENDING);
                notificationService.createNotification(userId,
                        "Your loan application has been submitted successfully", "APPLICATION_SUBMITTED");
            }

            LoanApplication savedApplication = loanRepo.save(application);

            // --- SAVE DOCUMENTS FOR ADMIN ACCESS ---
            if (pfAccountPdf != null && !pfAccountPdf.isEmpty()) {
                documentService.saveDocument(pfAccountPdf, userId, "PF_ACCOUNT_PDF");
            }
            if (salarySlip != null && !salarySlip.isEmpty()) {
                documentService.saveDocument(salarySlip, userId, "SALARY_SLIP");
            }

            return savedApplication;
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit loan application: " + e.getMessage(), e);
        }
    }

    // Deterministic, realistic credit score generation based on PAN
    private int fetchCreditScoreByPan(String panCard) {
        int hash = Math.abs(panCard.toUpperCase().hashCode());
        // Credit score between 550 and 850
        return 550 + (hash % 301);
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> getApplicationsByUserId(Long userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }
            return loanRepo.findByUser_Id(userId);
        } catch (Exception e) {
            System.err.println("Error fetching applications for user " + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> getAllApplications() {
        try {
            return loanRepo.findAll();
        } catch (Exception e) {
            System.err.println("Error fetching all applications: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public LoanApplication updateLoanStatus(String applicationId, ApplicationStatus status) {
        try {
            LoanApplication application = loanRepo.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            application.setStatus(status);

            notificationService.notifyLoanStatus(
                    application.getUser().getId(),
                    applicationId,
                    status
            );

            return loanRepo.save(application);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update loan status: " + e.getMessage(), e);
        }
    }

    // --- NEW: Get application by ID (for document download) ---
    @Transactional(readOnly = true)
    public LoanApplication getApplicationById(String applicationId) {
        // If your applicationId is a String UUID (as in your code), use as is.
        Optional<LoanApplication> appOpt = loanRepo.findById(applicationId);
        return appOpt.orElse(null);
    }
}