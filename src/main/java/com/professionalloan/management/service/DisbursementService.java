package com.professionalloan.management.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.professionalloan.management.model.Disbursement;
import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.DisbursementStatus;
import com.professionalloan.management.model.ApplicationStatus;
import com.professionalloan.management.model.User;
import com.professionalloan.management.repository.DisbursementRepository;
import com.professionalloan.management.repository.LoanApplicationRepository;

@Service
public class DisbursementService {

    @Autowired
    private DisbursementRepository disbursementRepo;

    @Autowired
    private LoanApplicationRepository loanRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RepaymentService repaymentService;

    public Disbursement disburseLoan(String applicationId, BigDecimal amount) {
        LoanApplication application = loanRepo.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Loan application not found"));

        if (!application.getStatus().equals(ApplicationStatus.APPROVED)) {
            throw new RuntimeException("Loan application must be approved before disbursement");
        }

        // Uncomment if you want to strictly match the approved loan amount
        // if (!amount.equals(application.getLoanAmount())) {
        //     throw new RuntimeException("Disbursement amount must match the approved loan amount");
        // }

        try {
            Disbursement disbursement = new Disbursement();
            disbursement.setLoanApplication(application);
            disbursement.setDisbursedAmount(amount);
            disbursement.setDisbursementDate(LocalDate.now());
            disbursement.setStatus(DisbursementStatus.PROCESSING);

            // Process the disbursement (bank transfer, etc.)
            processDisbursement(disbursement);

            disbursement.setStatus(DisbursementStatus.COMPLETED);
            application.setStatus(ApplicationStatus.DISBURSED);

            loanRepo.save(application);

            // Generate EMI schedule after disbursement
            repaymentService.generateEMISchedule(applicationId, application.getTenureInMonths());

            // --- Notify user (in-app and email) after disbursement ---
            User user = application.getUser();
            notificationService.notifyDisbursement(user.getId(), application.getApplicationId()); // In-app
            notificationService.sendDisbursementEmail(user, application.getApplicationId(), amount); // Email

            return disbursementRepo.save(disbursement);
        } catch (Exception e) {
            Disbursement failedDisbursement = new Disbursement();
            failedDisbursement.setLoanApplication(application);
            failedDisbursement.setStatus(DisbursementStatus.FAILED);
            disbursementRepo.save(failedDisbursement);
            throw new RuntimeException("Disbursement failed: " + e.getMessage());
        }
    }

    private void processDisbursement(Disbursement disbursement) {
        // Implement actual disbursement logic (bank transfer, etc.)
        // This is a placeholder for the actual implementation
        try {
            // Simulate processing time
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Disbursement processing interrupted");
        }
    }
}