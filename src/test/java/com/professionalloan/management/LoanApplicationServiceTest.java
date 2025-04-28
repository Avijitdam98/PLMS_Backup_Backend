package com.professionalloan.management;
import com.professionalloan.management.service.LoanApplicationService;
import com.professionalloan.management.service.NotificationService;
import com.professionalloan.management.service.DocumentService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.professionalloan.management.model.LoanApplication;
import com.professionalloan.management.model.ApplicationStatus;
import com.professionalloan.management.repository.LoanApplicationRepository;
import com.professionalloan.management.repository.UserRepository;


public class LoanApplicationServiceTest {

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetApplicationById() {
        LoanApplication loanApplication = new LoanApplication();
        loanApplication.setApplicationId("APP123");
        when(loanApplicationRepository.findById("APP123")).thenReturn(Optional.of(loanApplication));

        LoanApplication found = loanApplicationService.getApplicationById("APP123");
        assertNotNull(found);
        assertEquals("APP123", found.getApplicationId());
    }

    @Test
    void testUpdateLoanStatus() {
        LoanApplication loanApplication = new LoanApplication();
        loanApplication.setApplicationId("APP456");
        loanApplication.setStatus(ApplicationStatus.PENDING);

        when(loanApplicationRepository.findById("APP456")).thenReturn(Optional.of(loanApplication));
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(loanApplication);

        LoanApplication updated = loanApplicationService.updateLoanStatus("APP456", ApplicationStatus.APPROVED);
        assertEquals(ApplicationStatus.APPROVED, updated.getStatus());
    }
}
