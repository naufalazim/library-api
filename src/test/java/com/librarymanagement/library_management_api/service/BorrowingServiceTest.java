package com.librarymanagement.library_management_api.service;

import com.librarymanagement.library_management_api.dto.response.BorrowingResponse;
import com.librarymanagement.library_management_api.entity.BookCopy;
import com.librarymanagement.library_management_api.entity.Borrower;
import com.librarymanagement.library_management_api.entity.Borrowing;
import com.librarymanagement.library_management_api.exception.BadRequestException;
import com.librarymanagement.library_management_api.exception.ConflictException;
import com.librarymanagement.library_management_api.exception.ResourceNotFoundException;
import com.librarymanagement.library_management_api.repository.BookCopyRepository;
import com.librarymanagement.library_management_api.repository.BorrowerRepository;
import com.librarymanagement.library_management_api.repository.BorrowingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    private static final Long BOOK_ID = 10L;
    private static final Long BORROWER_ID = 20L;
    private static final Long OTHER_BORROWER_ID = 99L;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BorrowingService borrowingService;

    private BookCopy bookCopy;
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        bookCopy = BookCopy.builder().id(BOOK_ID).build();
        borrower = Borrower.builder().id(BORROWER_ID).name("Alice").email("alice@example.com").build();
    }

    @Nested
    @DisplayName("borrowBook")
    class BorrowBook {

        @Test
        @DisplayName("creates an active borrowing when book and borrower exist and book is available")
        void success() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
            when(borrowingRepository.existsByBookCopyIdAndReturnedAtIsNull(BOOK_ID)).thenReturn(false);
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(invocation -> {
                Borrowing b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            Instant before = Instant.now();
            BorrowingResponse response = borrowingService.borrowBook(BOOK_ID, BORROWER_ID);

            assertThat(response.borrowingId()).isEqualTo(1L);
            assertThat(response.bookId()).isEqualTo(BOOK_ID);
            assertThat(response.borrowerId()).isEqualTo(BORROWER_ID);
            assertThat(response.borrowedAt()).isNotNull().isAfterOrEqualTo(before);
            assertThat(response.returnedAt()).isNull();
            assertThat(response.status()).isEqualTo("BORROWED");

            ArgumentCaptor<Borrowing> captor = ArgumentCaptor.forClass(Borrowing.class);
            verify(borrowingRepository).save(captor.capture());
            Borrowing saved = captor.getValue();
            assertThat(saved.getBookCopy()).isSameAs(bookCopy);
            assertThat(saved.getBorrower()).isSameAs(borrower);
            assertThat(saved.getReturnedAt()).isNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void bookNotFound() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowingService.borrowBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Book with id " + BOOK_ID + " not found");

            verifyNoInteractions(borrowerRepository, borrowingRepository);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when borrower does not exist")
        void borrowerNotFound() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowingService.borrowBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Borrower with id " + BORROWER_ID + " not found");

            verifyNoInteractions(borrowingRepository);
        }

        @Test
        @DisplayName("throws ConflictException when book is already borrowed")
        void alreadyBorrowed() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
            when(borrowingRepository.existsByBookCopyIdAndReturnedAtIsNull(BOOK_ID)).thenReturn(true);

            assertThatThrownBy(() -> borrowingService.borrowBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Book with id " + BOOK_ID + " is already borrowed");

            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        @DisplayName("translates DataIntegrityViolationException (race condition) into ConflictException")
        void raceConditionOnSave() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
            when(borrowingRepository.existsByBookCopyIdAndReturnedAtIsNull(BOOK_ID)).thenReturn(false);
            when(borrowingRepository.save(any(Borrowing.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

            assertThatThrownBy(() -> borrowingService.borrowBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Book with id " + BOOK_ID + " is already borrowed");
        }
    }

    @Nested
    @DisplayName("returnBook")
    class ReturnBook {

        private Borrowing activeBorrowing;

        @BeforeEach
        void setUp() {
            activeBorrowing = Borrowing.builder()
                    .id(1L)
                    .bookCopy(bookCopy)
                    .borrower(borrower)
                    .borrowedAt(Instant.now().minusSeconds(3600))
                    .build();
        }

        @Test
        @DisplayName("marks the borrowing as returned when returned by the original borrower")
        void success() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowingRepository.findByBookCopyIdAndReturnedAtIsNull(BOOK_ID))
                    .thenReturn(Optional.of(activeBorrowing));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Instant before = Instant.now();
            BorrowingResponse response = borrowingService.returnBook(BOOK_ID, BORROWER_ID);

            assertThat(response.borrowingId()).isEqualTo(1L);
            assertThat(response.bookId()).isEqualTo(BOOK_ID);
            assertThat(response.borrowerId()).isEqualTo(BORROWER_ID);
            assertThat(response.borrowedAt()).isEqualTo(activeBorrowing.getBorrowedAt());
            assertThat(response.returnedAt()).isNotNull().isAfterOrEqualTo(before);
            assertThat(response.status()).isEqualTo("RETURNED");

            verify(borrowingRepository).save(activeBorrowing);
            assertThat(activeBorrowing.getReturnedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void bookNotFound() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowingService.returnBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Book with id " + BOOK_ID + " not found");

            verifyNoInteractions(borrowingRepository);
        }

        @Test
        @DisplayName("throws BadRequestException when book is not currently borrowed")
        void notCurrentlyBorrowed() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowingRepository.findByBookCopyIdAndReturnedAtIsNull(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowingService.returnBook(BOOK_ID, BORROWER_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Book with id " + BOOK_ID + " is not currently borrowed");

            verify(borrowingRepository, never()).save(any(Borrowing.class));
        }

        @Test
        @DisplayName("throws ConflictException when a different borrower tries to return the book")
        void differentBorrower() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowingRepository.findByBookCopyIdAndReturnedAtIsNull(BOOK_ID))
                    .thenReturn(Optional.of(activeBorrowing));

            assertThatThrownBy(() -> borrowingService.returnBook(BOOK_ID, OTHER_BORROWER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Book with id " + BOOK_ID + " was borrowed by a different borrower");

            verify(borrowingRepository, never()).save(any(Borrowing.class));
            assertThat(activeBorrowing.getReturnedAt()).isNull();
        }

        @Test
        @DisplayName("does not consult the borrower repository")
        void doesNotLookUpBorrower() {
            when(bookCopyRepository.findById(BOOK_ID)).thenReturn(Optional.of(bookCopy));
            when(borrowingRepository.findByBookCopyIdAndReturnedAtIsNull(BOOK_ID))
                    .thenReturn(Optional.of(activeBorrowing));
            when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(invocation -> invocation.getArgument(0));

            borrowingService.returnBook(BOOK_ID, BORROWER_ID);

            verify(borrowerRepository, never()).findById(anyLong());
        }
    }
}
