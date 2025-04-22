package com.professionalloan.management.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.Repayment;
import com.professionalloan.management.repository.LoanApplicationRepository;
import com.professionalloan.management.repository.RepaymentRepository;
import com.professionalloan.management.dto.RepaymentDTO;

@Service
public class RepaymentService {

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    // Calculate EMI amount
    public BigDecimal calculateEMI(BigDecimal principal, int tenureInMonths, double interestRate) {
        // Convert annual interest rate to monthly
        double monthlyRate = (interestRate / 12.0) / 100.0;

        // EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
        double emi = principal.doubleValue() * monthlyRate *
                Math.pow(1 + monthlyRate, tenureInMonths) /
                (Math.pow(1 + monthlyRate, tenureInMonths) - 1);

        return new BigDecimal(emi).setScale(2, RoundingMode.HALF_UP);
    }

    // Generate EMI schedule after loan disbursement
    public List<Repayment> generateEMISchedule(String applicationId, int tenureInMonths) {
        LoanApplication loan = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Loan application not found"));

        BigDecimal emiAmount = calculateEMI(loan.getLoanAmount(), tenureInMonths, 12.0); // 12% annual interest
        List<Repayment> emiSchedule = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        // Use a placeholder date for unpaid EMIs to satisfy NOT NULL constraint
        // This date is far in the future and will be updated when payment is made
        LocalDate placeholderPaidDate = LocalDate.of(2099, 12, 31);

        for (int i = 1; i <= tenureInMonths; i++) {
            Repayment repayment = new Repayment();
            repayment.setLoanApplication(loan);
            repayment.setEmiAmount(emiAmount);
            repayment.setEmiNumber(i);
            repayment.setDueDate(startDate.plusMonths(i));
            repayment.setStatus("PENDING");
            // Set a placeholder date to avoid NOT NULL constraint
            repayment.setPaidDate(placeholderPaidDate);
            emiSchedule.add(repayment);
        }

        return repaymentRepository.saveAll(emiSchedule);
    }

    // Make EMI payment
    public Repayment makePayment(Long repaymentId) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));

        if ("PAID".equals(repayment.getStatus())) {
            throw new RuntimeException("EMI already paid");
        }

        repayment.setStatus("PAID");
        repayment.setPaidDate(LocalDate.now());
        return repaymentRepository.save(repayment);
    }

    // Get all EMIs for a loan
    public List<Repayment> getLoanEMIs(String applicationId) {
        return repaymentRepository.findByLoanApplication_ApplicationId(applicationId);
    }

    // Get pending EMIs for a loan
    public List<Repayment> getPendingEMIs(String applicationId) {
        return repaymentRepository.findByLoanApplication_ApplicationIdAndStatus(applicationId, "PENDING");
    }

    // Update EMI status (for overdue EMIs)
    public void updateEMIStatuses() {
        List<Repayment> pendingEMIs = repaymentRepository.findByLoanApplication_ApplicationIdAndStatus(null, "PENDING");
        LocalDate today = LocalDate.now();

        for (Repayment emi : pendingEMIs) {
            if (emi.getDueDate().isBefore(today)) {
                emi.setStatus("OVERDUE");
                repaymentRepository.save(emi);
            }
        }
    }

    // Map Repayment entity to RepaymentDTO for API responses
    public RepaymentDTO toDTO(Repayment repayment) {
        RepaymentDTO dto = new RepaymentDTO();
        dto.setId(repayment.getId());
        dto.setEmiAmount(repayment.getEmiAmount());
        dto.setDueDate(repayment.getDueDate());
        dto.setPaidDate(repayment.getPaidDate());
        dto.setStatus(repayment.getStatus());
        dto.setEmiNumber(repayment.getEmiNumber());
        dto.setApplicationId(
            repayment.getLoanApplication() != null ? repayment.getLoanApplication().getApplicationId() : null
        );
        return dto;
    }
}