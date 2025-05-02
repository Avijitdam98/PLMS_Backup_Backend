package com.professionalloan.management.controller;

import com.professionalloan.management.model.Repayment;
import com.professionalloan.management.service.RepaymentService;
import com.professionalloan.management.dto.RepaymentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repayments")
@CrossOrigin(origins = "http://localhost:5173")
public class RepaymentController {

    @Autowired
    private RepaymentService repaymentService;

    //  Simulate EMI payment with payment method (GPay, PhonePe, Card)
    @PostMapping("/simulatepay")
    public ResponseEntity<?> simulatePayment(
            @RequestParam Long repaymentId,
            @RequestParam String method 
    ) {
    	Repayment repaid = repaymentService.simulatePayment(repaymentId, method);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Payment successful");
        response.put("transactionId", repaid.getTransactionId());
        response.put("paymentMethod", repaid.getPaymentMethod());
        return ResponseEntity.ok(response);
    }

    //  Pay a single EMI (manual trigger, still works)
    @PostMapping("/pay/{repaymentId}")
    public ResponseEntity<Map<String, String>> paySingleEMI(@PathVariable Long repaymentId) {
        repaymentService.makePayment(repaymentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "EMI paid successfully");
        return ResponseEntity.ok(response);
    }

    //  Get all EMIs for a specific loan
    @GetMapping("/loan/{applicationId}")
    public ResponseEntity<List<RepaymentDTO>> getLoanEMIs(@PathVariable String applicationId) {
        List<Repayment> emis = repaymentService.getLoanEMIs(applicationId);
        List<RepaymentDTO> dtoList = emis.stream()
                .map(repaymentService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

//    //  Get only pending EMIs for a loan
//    @GetMapping("/loan/{applicationId}/pending")
//    public ResponseEntity<List<RepaymentDTO>> getPendingEMIs(@PathVariable String applicationId) {
//        List<Repayment> pendingEmis = repaymentService.getPendingEMIs(applicationId);
//        List<RepaymentDTO> dtoList = pendingEmis.stream()
//                .map(repaymentService::toDTO)
//                .collect(Collectors.toList());
//        return ResponseEntity.ok(dtoList);
//    }

    // ✅ Get all repayments across all loans for a user (for admin)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RepaymentDTO>> getRepaymentsByUser(@PathVariable Long userId) {
        List<Repayment> repayments = repaymentService.getRepaymentsByUserId(userId);
        List<RepaymentDTO> dtos = repayments.stream()
                .map(repaymentService::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
