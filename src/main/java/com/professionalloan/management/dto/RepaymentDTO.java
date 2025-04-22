package com.professionalloan.management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepaymentDTO {
    private Long id;
    private BigDecimal emiAmount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String status;
    private Integer emiNumber;
    private String applicationId;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getEmiAmount() { return emiAmount; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEmiNumber() { return emiNumber; }
    public void setEmiNumber(Integer emiNumber) { this.emiNumber = emiNumber; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
}