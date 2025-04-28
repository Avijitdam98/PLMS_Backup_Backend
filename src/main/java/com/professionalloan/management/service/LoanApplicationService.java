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

    // ✅ Submit loan application safely
    @Transactional
    public LoanApplication submitApplicationWithFiles(
            String name,
            String profession,
            String purpose,
            BigDecimal loanAmount,
            String panCard,
            Integer tenureInMonths,
            Long userId,
            MultipartFile pfAccountPdf,
            MultipartFile salarySlip
    ) {
        try {
            // ✅ Validate User
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Check if user already has an active loan (approved/disbursed)
            List<LoanApplication> existingApps = loanRepo.findByUser_Id(userId);
            for (LoanApplication app : existingApps) {
                if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.DISBURSED) {
                    throw new RuntimeException("You already have an active loan. Cannot apply again until it is closed.");
                }
            }

            // ✅ Check if PAN card already used in system (simple duplicate check)
            for (LoanApplication app : existingApps) {
                if (app.getPanCard().equalsIgnoreCase(panCard)) {
                    throw new RuntimeException("PAN card already exists in another application.");
                }
            }

            // ✅ Generate credit score
            int creditScore = fetchCreditScoreByPan(panCard);

            // ✅ Create new application
            LoanApplication application = new LoanApplication();
            application.setApplicationId(UUID.randomUUID().toString());
            application.setName(name);
            application.setProfession(profession);
            application.setPurpose(purpose);
            application.setLoanAmount(loanAmount);
            application.setCreditScore(creditScore);
            application.setPanCard(panCard);
            application.setTenureInMonths(tenureInMonths);
            application.setUser(user);

            // ✅ Attach PDFs (stored in DB temporarily)
            application.setPfAccountPdf(pfAccountPdf != null && !pfAccountPdf.isEmpty() ? pfAccountPdf.getBytes() : null);
            application.setSalarySlip(salarySlip != null && !salarySlip.isEmpty() ? salarySlip.getBytes() : null);

            // ✅ Decide Status
            if (creditScore < MINIMUM_CREDIT_SCORE) {
                application.setStatus(ApplicationStatus.REJECTED);
                notificationService.notifyLoanStatus(userId, application.getApplicationId(), ApplicationStatus.REJECTED);
            } else {
                application.setStatus(ApplicationStatus.PENDING);
                notificationService.createNotification(userId,
                        "Your loan application has been submitted successfully!", "APPLICATION_SUBMITTED");
            }

            // ✅ Save application
            LoanApplication savedApplication = loanRepo.save(application);

            // ✅ Save Documents separately for admin access
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

    // ✅ Generate credit score from PAN (realistic simulation)
    private int fetchCreditScoreByPan(String panCard) {
        int hash = Math.abs(panCard.toUpperCase().hashCode());
        return 550 + (hash % 301); // 550 - 850 range
    }

    // ✅ Get Applications by User
    @Transactional(readOnly = true)
    public List<LoanApplication> getApplicationsByUserId(Long userId) {
        try {
            if (userId == null) {
                return Collections.emptyList();
            }
            return loanRepo.findByUser_Id(userId);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching applications for user: " + e.getMessage());
        }
    }

    // ✅ Get All Applications
    @Transactional(readOnly = true)
    public List<LoanApplication> getAllApplications() {
        try {
            return loanRepo.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching all applications: " + e.getMessage());
        }
    }

    // ✅ Update Loan Status
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

    // ✅ Get Application by ID
    @Transactional(readOnly = true)
    public LoanApplication getApplicationById(String applicationId) {
        return loanRepo.findById(applicationId).orElse(null);
    }
}
