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
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BorrowingService {

    private final BorrowingRepository borrowingRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowerRepository borrowerRepository;

    public BorrowingService(BorrowingRepository borrowingRepository, BookCopyRepository bookCopyRepository, BorrowerRepository borrowerRepository) {
        this.borrowingRepository = borrowingRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public BorrowingResponse borrowBook(Long bookId, Long borrowerId) {
        // Validate both resources exist
        BookCopy copy = bookCopyRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book with id " + bookId + " not found"
                ));

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Borrower with id " + borrowerId + " not found"
                ));

        // Business rule: this copy must not already be borrowed
        if (borrowingRepository.existsByBookCopyIdAndReturnedAtIsNull(bookId)) {
            throw new ConflictException(
                    "Book with id " + bookId + " is already borrowed"
            );
        }

        // Create the borrowing record
        Borrowing borrowing = Borrowing.builder()
                .bookCopy(copy)
                .borrower(borrower)
                .borrowedAt(Instant.now())
                .build();

        try {
            Borrowing saved = borrowingRepository.save(borrowing);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Race condition protection: 2 people borrowing same book at same time
            throw new ConflictException(
                    "Book with id " + bookId + " is already borrowed"
            );
        }
    }

    @Transactional
    public BorrowingResponse returnBook(Long bookId, Long borrowerId) {
        // validate book exists
        bookCopyRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException(
                "Book with id " + bookId + " not found"
        ));

        // find the active borrowing for this copy
        Borrowing borrowing = borrowingRepository.findByBookCopyIdAndReturnedAtIsNull(bookId)
                .orElseThrow(() -> new BadRequestException(
                        "Book with id " + bookId + " is not currently borrowed"
                ));

        // only the borrower who borrowed it may return it
        if (!borrowing.getBorrower().getId().equals(borrowerId)) {
            throw new ConflictException(
                    "Book with id " + bookId + " was borrowed by a different borrower"
            );
        }

        // Mark as returned
        borrowing.setReturnedAt(Instant.now());
        Borrowing saved = borrowingRepository.save(borrowing);
        return toResponse(saved);
    }

    private BorrowingResponse toResponse(Borrowing b) {
        return new BorrowingResponse(
                b.getId(),
                b.getBookCopy().getId(),
                b.getBorrower().getId(),
                b.getBorrowedAt(),
                b.getReturnedAt(),
                b.getReturnedAt() == null ? "BORROWED" : "RETURNED"
        );
    }
}