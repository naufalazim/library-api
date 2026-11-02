package com.librarymanagement.library_management_api.service;

import com.librarymanagement.library_management_api.dto.request.CreateBorrowerRequest;
import com.librarymanagement.library_management_api.dto.response.BorrowerResponse;
import com.librarymanagement.library_management_api.entity.Borrower;
import com.librarymanagement.library_management_api.exception.ConflictException;
import com.librarymanagement.library_management_api.repository.BorrowerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BorrowerService borrowerService;

    @Test
    @DisplayName("registerBorrower saves a new borrower and returns the response")
    void registerBorrower_success() {
        CreateBorrowerRequest request = new CreateBorrowerRequest("Naufal", "naufal@example.com");

        when(borrowerRepository.existsByEmail("naufal@example.com")).thenReturn(false);
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
            Borrower b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BorrowerResponse response = borrowerService.registerBorrower(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Naufal");
        assertThat(response.email()).isEqualTo("naufal@example.com");

        ArgumentCaptor<Borrower> captor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(captor.capture());
        Borrower saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Naufal");
        assertThat(saved.getEmail()).isEqualTo("naufal@example.com");
    }

    @Test
    @DisplayName("registerBorrower throws ConflictException when email already exists")
    void registerBorrower_duplicateEmail_throwsConflict() {
        CreateBorrowerRequest request = new CreateBorrowerRequest("Naufal", "naufal@example.com");

        when(borrowerRepository.existsByEmail("naufal@example.com")).thenReturn(true);

        assertThatThrownBy(() -> borrowerService.registerBorrower(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A borrower with Email naufal@example.com already exists");

        verify(borrowerRepository, never()).save(any(Borrower.class));
    }
}
