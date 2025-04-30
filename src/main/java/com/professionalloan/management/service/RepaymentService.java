package com.professionalloan.management.service;

import com.professionalloan.management.model.ApplicationStatus;
import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.Repayment;
import com.professionalloan.management.repository.LoanApplicationRepository;
import com.professionalloan.management.repository.RepaymentRepository;
import com.professionalloan.management.dto.RepaymentDTO;
import com.professionalloan.management.exception.LoanApplicationNotFoundException;
import com.professionalloan.management.exception.RepaymentNotFoundException;
import com.professionalloan.management.exception.EMIAlreadyPaidException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepaymentService {

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    // Get Repayment by ID
    public Repayment getRepaymentById(Long repaymentId) {
        return repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RepaymentNotFoundException("Repayment not found with ID: " + repaymentId));
    }

    //  Calculate EMI
    public BigDecimal calculateEMI(BigDecimal principal, int tenureInMonths, double interestRate) {
        double monthlyRate = (interestRate / 12.0) / 100.0;
        double emi = principal.doubleValue() * monthlyRate * Math.pow(1 + monthlyRate, tenureInMonths) /
                (Math.pow(1 + monthlyRate, tenureInMonths) - 1);
        return new BigDecimal(emi).setScale(2, RoundingMode.HALF_UP);
    }

    //  Generate EMI Schedule after Disbursement
    public List<Repayment> generateEMISchedule(String applicationId, int tenureInMonths) {
        LoanApplication loan = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application not found with ID: " + applicationId));

        BigDecimal emiAmount = calculateEMI(loan.getLoanAmount(), tenureInMonths, 12.0); 
        List<Repayment> emiSchedule = new ArrayList<>();
        LocalDate startDate = LocalDate.now();

        for (int i = 1; i <= tenureInMonths; i++) {
            Repayment repayment = new Repayment();
            repayment.setLoanApplication(loan);
            repayment.setEmiAmount(emiAmount);
            repayment.setEmiNumber(i);
            repayment.setDueDate(startDate.plusMonths(i));
            repayment.setStatus("PENDING");
            repayment.setPaidDate(null);
            emiSchedule.add(repayment);
        }

        return repaymentRepository.saveAll(emiSchedule);
    }

    //  Pay a Single EMI
    @Transactional
    public Repayment makePayment(Long repaymentId) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RepaymentNotFoundException("Repayment not found with ID: " + repaymentId));

        if ("PAID".equalsIgnoreCase(repayment.getStatus())) {
            throw new EMIAlreadyPaidException("This EMI is already paid");
        }

        repayment.setStatus("PAID");
        repayment.setPaidDate(LocalDate.now());
        repaymentRepository.save(repayment);

        // After paying this EMI, check if loan can be closed
        LoanApplication loan = repayment.getLoanApplication();
        List<Repayment> pendingEmis = repaymentRepository
                .findByLoanApplication_ApplicationIdAndStatus(loan.getApplicationId(), "PENDING");

        if (pendingEmis.isEmpty()) {
            loan.setStatus(ApplicationStatus.CLOSED);
            loanApplicationRepository.save(loan);
        }

        return repayment;
    }
    
    
//  Get all EMIs for a Loan
    public List<Repayment> getLoanEMIs(String applicationId) {
        return repaymentRepository.findByLoanApplication_ApplicationId(applicationId);
    }

    //  Get Pending EMIs for a Loan
    public List<Repayment> getPendingEMIs(String applicationId) {
        return repaymentRepository.findByLoanApplication_ApplicationIdAndStatus(applicationId, "PENDING");
    }
    
    
    
//  Mapping Repayment Entity to DTO
    public RepaymentDTO toDTO(Repayment repayment) {
        RepaymentDTO dto = new RepaymentDTO();
        dto.setId(repayment.getId());
        dto.setEmiAmount(repayment.getEmiAmount());
        dto.setDueDate(repayment.getDueDate());
        dto.setPaidDate(repayment.getPaidDate());
        dto.setStatus(repayment.getStatus());
        dto.setEmiNumber(repayment.getEmiNumber());
        dto.setApplicationId(repayment.getLoanApplication() != null
                ? repayment.getLoanApplication().getApplicationId()
                : null);
        return dto;
    }
    
    
//  Get all repayments for all loans of a user
    public List<Repayment> getRepaymentsByUserId(Long userId) {
        return repaymentRepository.findByLoanApplication_User_Id(userId);
    }
    
    
    
    
    
    
    

    //  Pay all Pending EMIs for a Loan
//    @Transactional
//    public void payAllPendingEMIs(String applicationId) {
//        List<Repayment> pendingEmis = repaymentRepository
//                .findByLoanApplication_ApplicationIdAndStatus(applicationId, "PENDING");
//
//        for (Repayment emi : pendingEmis) {
//            emi.setStatus("PAID");
//            emi.setPaidDate(LocalDate.now());
//        }
//        repaymentRepository.saveAll(pendingEmis);
//
//        LoanApplication loan = loanApplicationRepository.findById(applicationId)
//                .orElseThrow(() -> new LoanApplicationNotFoundException("Loan application not found with ID: " + applicationId));
//        loan.setStatus(ApplicationStatus.CLOSED);
//        loanApplicationRepository.save(loan);
//    }

    

    //  Update EMI statuses to Overdue if Past Due Date
//    @Transactional
//    public void updateEMIStatuses() {
//        List<Repayment> pendingEmis = repaymentRepository.findByStatus("PENDING");
//        LocalDate today = LocalDate.now();
//
//        for (Repayment emi : pendingEmis) {
//            if (emi.getDueDate().isBefore(today) && !"PAID".equalsIgnoreCase(emi.getStatus())) {
//                emi.setStatus("OVERDUE");
//                repaymentRepository.save(emi);
//            }
//        }
//    }

    

    
}