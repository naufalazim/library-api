package com.librarymanagement.library_management_api.service;

import com.librarymanagement.library_management_api.dto.request.CreateBookRequest;
import com.librarymanagement.library_management_api.dto.response.BookResponse;
import com.librarymanagement.library_management_api.entity.BookCopy;
import com.librarymanagement.library_management_api.entity.BookTitle;
import com.librarymanagement.library_management_api.exception.ConflictException;
import com.librarymanagement.library_management_api.repository.BookCopyRepository;
import com.librarymanagement.library_management_api.repository.BookTitleRepository;
import com.librarymanagement.library_management_api.repository.BorrowingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- USE SPRING ONE

import java.util.List;

@Service
public class BookService {

    private final BookTitleRepository bookTitleRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowingRepository borrowingRepository;

    public BookService(BookTitleRepository bookTitleRepository, BookCopyRepository bookCopyRepository, BorrowingRepository borrowingRepository) {
        this.bookTitleRepository = bookTitleRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowingRepository = borrowingRepository;
    }

    @Transactional // for writing - readOnly = false by default
    public BookResponse registerBook(CreateBookRequest request) {
        BookTitle title = bookTitleRepository.findById(request.isbn())
                .map(existing -> {
                    if(!existing.getTitle().equals(request.title()) || !existing.getAuthor().equals(request.author())) {
                        throw new ConflictException(
                                "ISBN " + request.isbn() + " already exists with a different title/author. Same ISBN must have same title and author."
                        );
                    }
                    return existing;
                })
                .orElseGet(() ->
                        bookTitleRepository.save(BookTitle.builder()
                                .isbn(request.isbn())
                                .title(request.title())
                                .author(request.author())
                                .build()));

        BookCopy copy = bookCopyRepository.save(BookCopy.builder()
                .bookTitle(title)
                .build()
        );

        return toBookResponse(copy, true);
    }

    @Transactional(readOnly = true) // now this works!
    public List<BookResponse> getAllBooks() {
        return bookCopyRepository.findAll().stream()
                .map(copy -> {
                    boolean available = !borrowingRepository.existsByBookCopyIdAndReturnedAtIsNull(copy.getId());
                    return toBookResponse(copy, available);
                })
                .toList();
    }

    private BookResponse toBookResponse(BookCopy copy, boolean available) {
        BookTitle t = copy.getBookTitle();
        return new BookResponse(
                copy.getId(),
                t.getIsbn(),
                t.getTitle(),
                t.getAuthor(),
                available
        );
    }
}