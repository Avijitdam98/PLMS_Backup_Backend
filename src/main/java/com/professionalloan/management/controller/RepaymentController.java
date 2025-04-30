package com.professionalloan.management.controller;

import com.professionalloan.management.model.Repayment;
import com.professionalloan.management.service.RepaymentService;
import com.professionalloan.management.dto.RepaymentDTO; // Import your DTO class!
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

    //  Pay a single EMI
    @PostMapping("/pay/{repaymentId}")
    public ResponseEntity<Map<String, String>> paySingleEMI(@PathVariable Long repaymentId) {
        repaymentService.makePayment(repaymentId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "EMI paid successfully");
        return ResponseEntity.ok(response);
    }

    // 2. Fetch all EMIs for a specific loan
    @GetMapping("/loan/{applicationId}")
    public ResponseEntity<List<RepaymentDTO>> getLoanEMIs(@PathVariable String applicationId) {
        List<Repayment> emis = repaymentService.getLoanEMIs(applicationId);
        List<RepaymentDTO> dtoList = emis.stream()
            .map(repaymentService::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // 3. Fetch pending EMIs only
    @GetMapping("/loan/{applicationId}/pending")
    public ResponseEntity<List<RepaymentDTO>> getPendingEMIs(@PathVariable String applicationId) {
        List<Repayment> pendingEmis = repaymentService.getPendingEMIs(applicationId);
        List<RepaymentDTO> dtoList = pendingEmis.stream()
            .map(repaymentService::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

//    //  Pay all pending EMIs together
//    @PostMapping("/pay-all/{applicationId}")
//    public ResponseEntity<Map<String, String>> payAllPendingEMIs(@PathVariable String applicationId) {
//        repaymentService.payAllPendingEMIs(applicationId);
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "All pending EMIs paid successfully and loan closed.");
//        return ResponseEntity.ok(response);
//    }

    // 5. Update overdue EMIs
//    @PutMapping("/update-statuses")
//    public ResponseEntity<Map<String, String>> updateEMIStatuses() {
//        repaymentService.updateEMIStatuses();
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "EMI statuses updated (overdue marked). Done successfully.");
//        return ResponseEntity.ok(response);
//    }

    //  Get all repayments for all loans of a user (ADMIN)
    @GetMapping("/user/{userId}")
    // @PreAuthorize("hasRole('ADMIN')") // Uncomment if using Spring Security
    public ResponseEntity<List<RepaymentDTO>> getRepaymentsByUser(@PathVariable Long userId) {
        List<Repayment> repayments = repaymentService.getRepaymentsByUserId(userId);
        List<RepaymentDTO> dtos = repayments.stream()
            .map(repaymentService::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}